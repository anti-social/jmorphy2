package net.uaprom.jmorphy2.solr;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import java.util.List;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import net.uaprom.jmorphy2.test._BaseTestCase;


@RunWith(JUnit4.class)
public class Jmorphy2AnalyzerTest extends BaseFilterTestCase {
    @Before
    public void setUp() throws IOException {
        initMorphAnalyzer();
    }

    @Test
    public void test() throws IOException {
        Analyzer analyzer = new Jmorphy2Analyzer(LUCENE_VERSION, morph);

        assertAnalyzesTo(analyzer,
                         "",
                         Arrays.asList(new String[0]),
                         Arrays.asList(new Integer[0]));
        assertAnalyzesTo(analyzer,
                         "тест стеммера",
                         Arrays.asList(new String[]{"тест", "тесто", "стеммера"}),
                         Arrays.asList(new Integer[]{1, 0, 1}));
        assertAnalyzesTo(analyzer,
                         "iphone",
                         Arrays.asList(new String[]{"iphone"}),
                         Arrays.asList(new Integer[]{1}));
        assertAnalyzesTo(analyzer,
                         "теплые перчатки",
                         Arrays.asList(new String[]{"тёплый", "перчатка"}),
                         Arrays.asList(new Integer[]{1, 1}));
        assertAnalyzesTo(analyzer,
                         "магнит на холодильник",
                         Arrays.asList(new String[]{"магнит", "холодильник"}),
                         Arrays.asList(new Integer[]{1, 2}));
        assertAnalyzesTo(analyzer,
                         "купить технику",
                         Arrays.asList(new String[]{"купить", "техника", "техник"}),
                         Arrays.asList(new Integer[]{1, 1, 0}));
        assertAnalyzesTo(analyzer,
                         "мы любим Украину",
                         Arrays.asList(new String[]{"любим", "любимый", "любить", "украина"}),
                         Arrays.asList(new Integer[]{2, 0, 0, 1}));
    }
}
