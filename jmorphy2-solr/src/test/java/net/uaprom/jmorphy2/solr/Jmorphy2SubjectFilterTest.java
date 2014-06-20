package net.uaprom.jmorphy2.solr;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import net.uaprom.jmorphy2.nlp.Ruleset;
import net.uaprom.jmorphy2.nlp.Tagger;
import net.uaprom.jmorphy2.nlp.SimpleTagger;
import net.uaprom.jmorphy2.nlp.Parser;
import net.uaprom.jmorphy2.nlp.SimpleParser;
import net.uaprom.jmorphy2.nlp.SubjectExtractor;


@RunWith(JUnit4.class)
public class Jmorphy2SubjectFilterTest extends BaseFilterTestCase {
    private static final String TAGGER_RULES_RESOURCE = "/tagger_rules.txt";
    private static final String PARSER_RULES_RESOURCE = "/parser_rules.txt";

    private SubjectExtractor subjExtractor;

    @Before
    public void setUp() throws IOException {
        initMorphAnalyzer();

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

    @Override
    protected TokenFilter getTokenFilter(TokenStream source) {
        return new Jmorphy2SubjectFilter(source, subjExtractor);
    }

    @Test
    public void test() throws IOException {
        Analyzer analyzer = getAnalyzer();

        assertAnalyzesTo(analyzer,
                         "",
                         Arrays.asList(new String[0]),
                         Arrays.asList(new Integer[0]));
        assertAnalyzesTo(analyzer,
                         "iphone",
                         Arrays.asList(new String[]{"iphone"}),
                         Arrays.asList(new Integer[]{1}));
        assertAnalyzesTo(analyzer,
                         "теплые перчатки",
                         Arrays.asList(new String[]{"перчатка"}),
                         Arrays.asList(new Integer[]{2}));
        assertAnalyzesTo(analyzer,
                         "магнит на холодильник",
                         Arrays.asList(new String[]{"магнит"}),
                         Arrays.asList(new Integer[]{1}));
        assertAnalyzesTo(analyzer,
                         "чехол кожаный 5 for iphone 4",
                         Arrays.asList(new String[]{"чехол", "5"}),
                         Arrays.asList(new Integer[]{1, 2}));
    }
}
