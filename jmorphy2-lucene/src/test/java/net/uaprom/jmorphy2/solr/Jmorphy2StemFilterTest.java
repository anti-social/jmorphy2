package net.uaprom.jmorphy2.lucene;

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
import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertAnalyzesTo;

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
                TokenFilter filter = new Jmorphy2StemFilter(source, morph, null, includeTags, true, enablePositionIncrements);
                return new TokenStreamComponents(source, filter);
            }
        };
    }

    @Test
    public void test() throws IOException {
        Analyzer analyzer = getAnalyzer(true);

        assertAnalyzesTo(analyzer,
                         "",
                         new String[0],
                         new int[0]);
        assertAnalyzesTo(analyzer,
                         "тест стеммера",
                         new String[]{"тест", "тесто", "стеммера"},
                         new int[]{1, 0, 1});
        assertAnalyzesTo(analyzer,
                         "iphone",
                         new String[]{"iphone"},
                         new int[]{1});
        assertAnalyzesTo(analyzer,
                         "теплые перчатки",
                         new String[]{"тёплый", "перчатка"},
                         new int[]{1, 1});
        assertAnalyzesTo(analyzer,
                         "магнит на холодильник",
                         new String[]{"магнит", "холодильник"},
                         new int[]{1, 2});
        assertAnalyzesTo(analyzer,
                         "купить технику",
                         new String[]{"техника", "техник"},
                         new int[]{2, 0});
    }

    @Test
    public void testDisablePositionIncrements() throws IOException {
        Analyzer analyzer = getAnalyzer(false);

        assertAnalyzesTo(analyzer,
                         "",
                         new String[0],
                         new int[0]);
        assertAnalyzesTo(analyzer,
                         "тест стеммера",
                         new String[]{"тест", "тесто", "стеммера"},
                         new int[]{1, 0, 1});
        assertAnalyzesTo(analyzer,
                         "iphone",
                         new String[]{"iphone"},
                         new int[]{1});
        assertAnalyzesTo(analyzer,
                         "теплые перчатки",
                         new String[]{"тёплый", "перчатка"},
                         new int[]{1, 1});
        assertAnalyzesTo(analyzer,
                         "магнит на холодильник",
                         new String[]{"магнит", "холодильник"},
                         new int[]{1, 1});
        assertAnalyzesTo(analyzer,
                         "купить технику",
                         new String[]{"техника", "техник"},
                         new int[]{1, 0});
    }
}
