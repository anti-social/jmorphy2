package net.uaprom.jmorphy2.elasticsearch.index;

import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;

import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;

import org.apache.lucene.analysis.Analyzer;
import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertAnalyzesTo;

import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.common.settings.Settings;


public class Jmorphy2StemTokenFilterFactoryTest extends BaseTokenFilterFactoryTest {
    @Test
    public void test() throws IOException {
        Settings settings = Settings.settingsBuilder()
            .put("path.home", createTempDir().toString())
            .put("path.conf", getDataPath("/indices/analyze/config"))
            .put("index.analysis.filter.jmorphy2.type", "jmorphy2_stemmer")
            .put("index.analysis.filter.jmorphy2.name", "ru")
            .put("index.analysis.filter.jmorphy2.exclude_tags", "NPRO PREP CONJ PRCL INTJ")
            .put("index.analysis.analyzer.text.tokenizer", "standard")
            .put("index.analysis.analyzer.text.filter", "jmorphy2")
            .build();

        AnalysisService analysisService = createAnalysisService(settings);
        TokenFilterFactory filterFactory = analysisService.tokenFilter("jmorphy2");
        assertThat(filterFactory, instanceOf(Jmorphy2StemTokenFilterFactory.class));
        Analyzer analyzer = analysisService.analyzer("text");
        
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
                         new int[]{2, 0, 0, 1, 0});
    }
}
