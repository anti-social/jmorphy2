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
    protected Query getUserQuery(String userQuery,
                                 SolrPluginUtils.DisjunctionMaxQueryParser up,
                                 SolrParams solrParams)
        throws SyntaxError {

        // System.out.println(">>> getUserQuery");
        // System.out.println(userQuery);
        Query q = super.getUserQuery(userQuery, up, solrParams);
        // System.out.println(q);
        // System.out.println("<<< getUserQuery");
        return q;
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
            // System.out.println("SimpleDisjunctionMaxQueryParser");
            // System.out.println(defaultField);
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
        };

        @Override
        public Query parse(String queryText) throws SyntaxError {
            // System.out.println(">>> parse");
            // System.out.println(queryText);
            int maxPos = 0;
            Map<Integer,List<FieldTerms>> allTerms = new HashMap<Integer,List<FieldTerms>>();
            // Map<Integer,List<BooleanQuery>> booleanQueries = new HashMap<Integer,List<BooleanQuery>>();
            for (String aliasedField : aliases.keySet()) {
                Alias alias = aliases.get(aliasedField);
                for (String field : alias.fields.keySet()) {
                    // System.out.println(field);
                    // System.out.println(alias.fields);
                    // System.out.println(alias.fields.get(field));
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
                        // BooleanQuery curQuery = null;
                        buffer.reset();
                        while (source.incrementToken()) {
                            termAtt.fillBytesRef();
                            int posInc = posIncAtt.getPositionIncrement();
                            if (curTerms == null || posInc != 0) {
                                curTerms = new FieldTerms(field, boost);
                            }
                            BytesRef term = BytesRef.deepCopyOf(bytes);
                            // System.out.println(term.utf8ToString());
                            curTerms.addTerm(term);
                            pos += posInc;
                            if (!allTerms.containsKey(pos)) {
                                allTerms.put(pos, new ArrayList<FieldTerms>(1));
                            }
                            allTerms.get(pos).add(curTerms);

                            // if (posInc == 0 && curQuery != null) {
                            //     curQuery = newBooleanQuery(true);
                            //     curQuery.add();
                            // } else {
                            //     curQuery = newTermQuery(new Term(field, BytesRef.deepCopyOf(bytes)));
                            // }
                            // pos += posInc;
                            // if (!booleanQueries.contains(pos)) {
                            //     booleanQuery.put(pos, new ArrayList<BooleanQuery>(1));
                            // }
                            // booleanQueries.get(pos).add(curQuery);
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

            // Query q = super.parse(queryText);
            // System.out.println(q);
            return q;
        }

        @Override
        protected Query getFieldQuery(String field, String queryText, boolean quoted)
            throws SyntaxError {

            // System.out.println(">>> getFieldQuery");
            // for (Map.Entry<String,Alias> e : aliases.entrySet()) {
            //     System.out.println(String.format("%s: %s", e.getKey(), e.getValue().fields));
            // }
            // System.out.println(field);
            // System.out.println(queryText);
            // System.out.println(getAnalyzer());
            Query q = super.getFieldQuery(field, queryText, quoted);
            // System.out.println(q);
            // System.out.println("<<< getFieldQuery");

            return q;
        }
    };
}
