package company.evo.jmorphy2.lucene;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;

import com.carrotsearch.randomizedtesting.RandomizedRunner;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertAnalyzesTo;

import company.evo.jmorphy2.nlp.Ruleset;
import company.evo.jmorphy2.nlp.Tagger;
import company.evo.jmorphy2.nlp.SimpleTagger;
import company.evo.jmorphy2.nlp.Parser;
import company.evo.jmorphy2.nlp.SimpleParser;
import company.evo.jmorphy2.nlp.SubjectExtractor;


@RunWith(RandomizedRunner.class)
public class Jmorphy2SubjectFilterTest extends BaseFilterTestCase {
    private static final String TAGGER_RULES_RESOURCE = "/tagger_rules.txt";
    private static final String PARSER_RULES_RESOURCE = "/parser_rules.txt";

    private SubjectExtractor subjExtractor;

    @Before
    public void setUp() throws IOException {
        init();

        Tagger tagger =
            new SimpleTagger(morph,
                             new Ruleset(getClass().getResourceAsStream(TAGGER_RULES_RESOURCE)));
        Parser parser =
            new SimpleParser(morph,
                             tagger,
                             new Ruleset(getClass().getResourceAsStream(PARSER_RULES_RESOURCE)));
        subjExtractor =
            new SubjectExtractor(parser,
                                 "+NP,nomn +NP,accs -PP NOUN,nomn NOUN,accs LATN NUMB",
                                 true);
    }

    protected Analyzer getAnalyzer() {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new WhitespaceTokenizer();
                TokenFilter filter = new Jmorphy2SubjectFilter(source, subjExtractor);
                return new TokenStreamComponents(source, filter);
            }
        };
    }

    @Test
    public void test() throws IOException {
        Analyzer analyzer = getAnalyzer();

        assertAnalyzesTo(analyzer,
                         "",
                         new String[0],
                         new int[0]);
        assertAnalyzesTo(analyzer,
                         "iphone",
                         new String[]{"iphone"},
                         new int[]{1});
        assertAnalyzesTo(analyzer,
                         "теплые перчатки",
                         new String[]{"перчатка"},
                         new int[]{2});
        assertAnalyzesTo(analyzer,
                         "магнит на холодильник",
                         new String[]{"магнит"},
                         new int[]{1});
        assertAnalyzesTo(analyzer,
                         "чехол кожаный 5 for iphone 4",
                         new String[]{"чехол", "5"},
                         new int[]{1, 2});
    }
}
