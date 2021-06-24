package company.evo.jmorphy2.lucene;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import company.evo.jmorphy2.MorphAnalyzer;


public final class Jmorphy2Analyzer extends Analyzer {
    private static List<Set<String>> DEFAULT_EXCLUDE_TAGS = new ArrayList<Set<String>>();
    static {
        for (final String pos : new String[]{"NPRO", "PREP", "CONJ", "PRCL", "INTJ"}) {
            DEFAULT_EXCLUDE_TAGS.add(new HashSet<String>() {
                    { add(pos); }
                });
        }
    }

    private final MorphAnalyzer morph;

    public Jmorphy2Analyzer(MorphAnalyzer morph) {
        super();
        this.morph = morph;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = new LowerCaseFilter(source);
        result = new Jmorphy2StemFilter(result, morph, null, DEFAULT_EXCLUDE_TAGS, true);
        return new TokenStreamComponents(source, result);
    }
}
