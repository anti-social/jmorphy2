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
public class Jmorphy2StemFilterTest extends BaseFilterTestCase {
    private static List<Set<String>> includeTags =
        ImmutableList.<Set<String>>of(ImmutableSet.of("NOUN"),
                                      ImmutableSet.of("ADJF"),
                                      ImmutableSet.of("ADJS"),
                                      ImmutableSet.of("LATN"),
                                      ImmutableSet.of("NUMB"));

    @Before
    public void setUp() throws IOException {
        initMorphAnalyzer();
    }

    protected Analyzer getAnalyzer(final boolean enablePositionIncrements) {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
                Tokenizer source = new WhitespaceTokenizer(LUCENE_VERSION, reader);
                TokenFilter filter = new Jmorphy2StemFilter(source, morph, includeTags, true, enablePositionIncrements);
                return new TokenStreamComponents(source, filter);
            }
        };
    }

    @Test
    public void test() throws IOException {
        Analyzer analyzer = getAnalyzer(true);

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
                         Arrays.asList(new String[]{"техника", "техник"}),
                         Arrays.asList(new Integer[]{2, 0}));
    }

    @Test
    public void testDisablePositionIncrements() throws IOException {
        Analyzer analyzer = getAnalyzer(false);

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
                         Arrays.asList(new Integer[]{1, 1}));
        assertAnalyzesTo(analyzer,
                         "купить технику",
                         Arrays.asList(new String[]{"техника", "техник"}),
                         Arrays.asList(new Integer[]{1, 0}));
    }
}
