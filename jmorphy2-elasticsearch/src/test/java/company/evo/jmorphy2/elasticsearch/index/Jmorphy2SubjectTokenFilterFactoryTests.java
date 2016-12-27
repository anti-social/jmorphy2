package company.evo.jmorphy2.elasticsearch.index;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.Matchers.instanceOf;

import org.apache.lucene.analysis.Analyzer;
import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertAnalyzesTo;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.test.ESTestCase;

import company.evo.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin;
import static company.evo.jmorphy2.elasticsearch.index.Utils.copyFilesFromResources;


public class Jmorphy2SubjectTokenFilterFactoryTests extends ESTestCase {
    public void testSubjectTokenFilter() throws IOException {
        Path home = createTempDir();
        Settings settings = Settings.builder()
            .put(Environment.PATH_HOME_SETTING.getKey(), home.toString())
            .put(Environment.PATH_CONF_SETTING.getKey(), home.resolve("config"))
            .put("index.analysis.filter.jmorphy2_subject.type", "jmorphy2_subject")
            .put("index.analysis.filter.jmorphy2_subject.name", "ru")
            .put("index.analysis.analyzer.text.tokenizer", "standard")
            .put("index.analysis.analyzer.text.filter", "jmorphy2_subject")
            .build();

        copyFilesFromResources(settings, "ru");

        AnalysisJmorphy2Plugin plugin = new AnalysisJmorphy2Plugin(settings);
        TestAnalysis analysis = createTestAnalysis
            (new Index("test", "_na_"), settings, plugin);
        assertThat(analysis.tokenFilter.get("jmorphy2_subject"),
                   instanceOf(Jmorphy2SubjectTokenFilterFactory.class));

        Analyzer analyzer = analysis.indexAnalyzers.get("text").analyzer();
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
