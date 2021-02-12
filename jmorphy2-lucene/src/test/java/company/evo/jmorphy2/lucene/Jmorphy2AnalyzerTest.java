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
public class Jmorphy2AnalyzerTest extends BaseFilterTestCase {
    @Before
    public void setUp() throws IOException {
        init();
    }

    @Test
    public void test() throws IOException {
        Analyzer analyzer = new Jmorphy2Analyzer(morph);

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
                         new String[]{"купить", "техника", "техник"},
                         new int[]{1, 1, 0});
        assertAnalyzesTo(analyzer,
                         "мы любим Украину",
                         new String[]{"любим", "любимый", "любить", "украина"},
                         new int[]{2, 0, 0, 1});
    }
}
