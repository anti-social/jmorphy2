package company.evo.jmorphy2.elasticsearch.index;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;

import org.apache.lucene.analysis.Analyzer;
import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertAnalyzesTo;

import org.elasticsearch.Version;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.test.ESTestCase;

import company.evo.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin;


public class Jmorphy2StemTokenFilterFactoryTests extends ESTestCase {

    public void testJmorphy2StemTokenFilter() throws IOException {
        Path home = createTempDir();
        Settings settings = Settings.builder()
            .put(Environment.PATH_HOME_SETTING.getKey(), home.toString())
            .put(Environment.PATH_CONF_SETTING.getKey(), home.resolve("config"))
            // .put(Environment.PATH_CONF_SETTING.getKey(), getDataPath("/indices/analyze/config"))
            .put("index.analysis.filter.jmorphy2.type", "jmorphy2_stemmer")
            .put("index.analysis.filter.jmorphy2.name", "ru")
            .put("index.analysis.filter.jmorphy2.exclude_tags", "NPRO PREP CONJ PRCL INTJ")
            .put("index.analysis.analyzer.text.tokenizer", "standard")
            .put("index.analysis.analyzer.text.filter", "jmorphy2")
            .build();

        TestAnalysis analysis = createAnalysis(settings);
        Analyzer analyzer = analysis.indexAnalyzers.get("text").analyzer();
        // TokenFilterFactory filterFactory = analysisService.tokenFilter("jmorphy2");
        // assertThat(filterFactory, instanceOf(Jmorphy2StemTokenFilterFactory.class));
        // Analyzer analyzer = analysisService.analyzer("text");

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

    private TestAnalysis createAnalysis(Settings settings) throws IOException {
        Path home = PathUtils.get(settings.get(Environment.PATH_HOME_SETTING.getKey()));
        Path ruPath = home.resolve("config/jmorphy2/ru");
        Path ruDictsPath = ruPath.resolve("pymorphy2_dicts");
        Files.createDirectories(ruDictsPath);
        copyResources("ru/pymorphy2_dicts", ruDictsPath);

        Settings nodeSettings = Settings.builder()
            .put(Environment.PATH_HOME_SETTING.getKey(), home)
            .build();
        System.out.println(settings.get(Environment.PATH_CONF_SETTING.getKey()));
        AnalysisJmorphy2Plugin plugin = new AnalysisJmorphy2Plugin(settings);
        return createTestAnalysis
            (new Index("test", "_na_"), nodeSettings, settings, plugin);
    }

    private void copyResources(String resourceDir, Path dest) throws IOException {
        InputStream dictFilesStream = getClass().getResourceAsStream(resourceDir);
        BufferedReader filesReader = new BufferedReader
            (new InputStreamReader(dictFilesStream, StandardCharsets.UTF_8));
        String resource;
        while ( (resource = filesReader.readLine()) != null ) {
            Files.copy
                (getClass().getResourceAsStream
                     (String.format(Locale.US, "%s/%s", resourceDir, resource)),
                 dest.resolve(resource));
        }
    }
}
