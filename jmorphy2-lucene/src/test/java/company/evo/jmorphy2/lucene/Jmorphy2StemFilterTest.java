package company.evo.jmorphy2.lucene;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;

import com.carrotsearch.randomizedtesting.RandomizedRunner;

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


@RunWith(RandomizedRunner.class)
public class Jmorphy2StemFilterTest extends BaseFilterTestCase {
    private static List<Set<String>> DEFAULT_INCLUDE_TAGS =
        List.<Set<String>>of(
            Set.of("NOUN"),
            Set.of("ADJF"),
            Set.of("ADJS"),
            Set.of("LATN"),
            Set.of("NUMB"),
            Set.of("UNKN")
        );

    @Before
    public void setUp() throws IOException {
        init();
    }

    protected Analyzer getAnalyzer(final List<Set<String>> includeTags,
                                   final List<Set<String>> excludeTags,
                                   final boolean enablePositionIncrements)
    {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new WhitespaceTokenizer();
                TokenFilter filter = new Jmorphy2StemFilter
                    (source, morph, includeTags, excludeTags, enablePositionIncrements);
                return new TokenStreamComponents(source, filter);
            }
        };
    }

    @Test
    public void test() throws IOException {
        Analyzer analyzer = getAnalyzer(DEFAULT_INCLUDE_TAGS, null, true);

        assertAnalyzesTo(analyzer,
                         "",
                         new String[0],
                         new int[0]);
        assertAnalyzesTo(analyzer,
                         "тест стеммера",
                         new String[]{"тест", "тесто", "стеммера", "стеммер"},
                         new int[]{1, 0, 1, 0});
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
        assertAnalyzesTo(analyzer,
                         "ъь ъё",
                         new String[]{"ъь", "ъё"},
                         new int[]{1, 1});
    }
 
    @Test
    public void testIgnoreUnknown() throws IOException {
        List<Set<String>> excludeUnknown = List.<Set<String>>of(Set.of("UNKN"));
        Analyzer analyzer = getAnalyzer(null, excludeUnknown, true);
        assertAnalyzesTo(analyzer,
                         "ъь ъё",
                         new String[0],
                         new int[0]);
    }

    @Test
    public void testSaveAll() throws IOException {
        Analyzer analyzer = getAnalyzer(null, null, true);
        assertAnalyzesTo(analyzer,
                         "ъь ъё",
                         new String[]{"ъь", "ъё"},
                         new int[]{1, 1});
    }

    @Test
    public void testDisablePositionIncrements() throws IOException {
        Analyzer analyzer = getAnalyzer(DEFAULT_INCLUDE_TAGS, null, false);

        assertAnalyzesTo(analyzer,
                         "",
                         new String[0],
                         new int[0]);
        assertAnalyzesTo(analyzer,
                         "тест стеммера",
                         new String[]{"тест", "тесто", "стеммера", "стеммер"},
                         new int[]{1, 0, 1, 0});
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
