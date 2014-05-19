package net.uaprom.jmorphy2.solr;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.solr.util.SolrPluginUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.DisMaxQParser;
import org.apache.solr.request.SolrQueryRequest;


public class SimpleDisMaxQParser extends DisMaxQParser {
    private static String IMPOSSIBLE_FIELD_NAME = "\uFFFC\uFFFC\uFFFC";

    public SimpleDisMaxQParser(String qstr,
                               SolrParams localParams,
                               SolrParams params,
                               SolrQueryRequest req) {
        super(qstr, localParams, params, req);
    }

    @Override
    protected SolrPluginUtils.DisjunctionMaxQueryParser
        getParser(Map<String, Float> fields,
                  String paramName,
                  SolrParams solrParams,
                  float tiebreaker) {

        // change parser only for query string, not phrase
        if (paramName == DisMaxParams.QS) {
            int slop = solrParams.getInt(paramName, 0);
            SolrPluginUtils.DisjunctionMaxQueryParser parser =
                new SimpleDisjunctionMaxQueryParser(this, IMPOSSIBLE_FIELD_NAME);
            parser.addAlias(IMPOSSIBLE_FIELD_NAME, tiebreaker, fields);
            parser.setPhraseSlop(slop);
            return parser;
        }

        return super.getParser(fields, paramName, solrParams, tiebreaker);
    }

    private static class SimpleDisjunctionMaxQueryParser extends SolrPluginUtils.DisjunctionMaxQueryParser {
        public SimpleDisjunctionMaxQueryParser(QParser qp, String defaultField) {
            super(qp, defaultField);
        }

        private static class FieldTerms {
            public final String field;
            public final Float boost;
            public final List<BytesRef> terms = new ArrayList<BytesRef>(2);

            public FieldTerms(String field, Float boost) {
                this.field = field;
                this.boost = boost;
            }

            public void addTerm(BytesRef bytes) {
                this.terms.add(bytes);
            }

            @Override
            public String toString() {
                return String.format("field: %s, terms: %s, boost: %s", field, terms.size(), boost);
            }
        };

        @Override
        public Query parse(String queryText) throws SyntaxError {
            int maxPos = 0;
            Map<Integer,List<FieldTerms>> allTerms = new HashMap<Integer,List<FieldTerms>>();

            for (String aliasedField : aliases.keySet()) {
                Alias alias = aliases.get(aliasedField);
                for (String field : alias.fields.keySet()) {
                    Float boost = alias.fields.get(field);
                    TokenStream source = null;
                    try {
                        source = getAnalyzer().tokenStream(field, queryText);
                        source.reset();
                        CachingTokenFilter buffer = new CachingTokenFilter(source);
                        TermToBytesRefAttribute termAtt =
                            buffer.getAttribute(TermToBytesRefAttribute.class);
                        PositionIncrementAttribute posIncAtt =
                            buffer.getAttribute(PositionIncrementAttribute.class);
                        BytesRef bytes = termAtt == null ? null : termAtt.getBytesRef();

                        int pos = 0;
                        FieldTerms curTerms = null;
                        buffer.reset();
                        while (source.incrementToken()) {
                            termAtt.fillBytesRef();
                            int posInc = posIncAtt.getPositionIncrement();
                            pos += posInc;
                            if (!allTerms.containsKey(pos)) {
                                allTerms.put(pos, new ArrayList<FieldTerms>());
                            }
                            if (curTerms == null || posInc != 0) {
                                curTerms = new FieldTerms(field, boost);
                                allTerms.get(pos).add(curTerms);
                            }
                            curTerms.addTerm(BytesRef.deepCopyOf(bytes));
                        }

                        maxPos = pos > maxPos ? pos : maxPos;
                    } catch (IOException e) {
                    } finally {
                        IOUtils.closeWhileHandlingException(source);
                    }
                    
                }
            }

            BooleanQuery q = newBooleanQuery(true);
            for (int i = 0; i <= maxPos; i++) {
                List<FieldTerms> terms = allTerms.get(i);
                if (terms == null) {
                    continue;
                }
                DisjunctionMaxQuery queryForTerm = new DisjunctionMaxQuery(0.0f);
                for (FieldTerms fieldTerms : terms) {
                    BooleanQuery fieldQuery = newBooleanQuery(true);
                    for (BytesRef term : fieldTerms.terms) {
                        fieldQuery.add(newTermQuery(new Term(fieldTerms.field, term)),
                                       BooleanClause.Occur.SHOULD);
                        if (fieldTerms.boost != null) {
                            fieldQuery.setBoost(fieldTerms.boost);
                        }
                    }
                    queryForTerm.add(fieldQuery);
                }
                
                q.add(queryForTerm, BooleanClause.Occur.SHOULD);
            }

            return q;
        }
    };
}
