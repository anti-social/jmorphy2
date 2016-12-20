package company.evo.jmorphy2.elasticsearch.index;

import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;

import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;

import org.apache.lucene.analysis.Analyzer;
import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertAnalyzesTo;

import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;

// import company.evo.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin;


public class Jmorphy2SubjectTokenFilterFactoryTests extends ESTestCase {
    public void testSubjectTokenFilter() throws IOException {
        Settings settings = Settings.builder()
            .put("path.home", createTempDir().toString())
            .put("path.conf", getDataPath("/indices/analyze/config"))
            .put("index.analysis.filter.jmorphy2_subject.type", "jmorphy2_subject")
            .put("index.analysis.filter.jmorphy2_subject.name", "ru")
            .put("index.analysis.analyzer.text.tokenizer", "standard")
            .put("index.analysis.analyzer.text.filter", "jmorphy2_subject")
            .build();

        // AnalysisService analysisService = createAnalysisService(new Index("test", "_na_"),
        //                                                         settings,
        //                                                         new AnalysisJmorphy2Plugin());
        // TokenFilterFactory filterFactory = analysisService.tokenFilter("jmorphy2_subject");
        // assertThat(filterFactory, instanceOf(Jmorphy2SubjectTokenFilterFactory.class));
        // Analyzer analyzer = analysisService.analyzer("text");

        // assertAnalyzesTo(analyzer,
        //                  "",
        //                  new String[0],
        //                  new int[0]);
        // assertAnalyzesTo(analyzer,
        //                  "iphone",
        //                  new String[]{"iphone"},
        //                  new int[]{1});
        // assertAnalyzesTo(analyzer,
        //                  "теплые перчатки",
        //                  new String[]{"перчатка"},
        //                  new int[]{2});
        // assertAnalyzesTo(analyzer,
        //                  "магнит на холодильник",
        //                  new String[]{"магнит"},
        //                  new int[]{1});
        // assertAnalyzesTo(analyzer,
        //                  "чехол кожаный 5 for iphone 4",
        //                  new String[]{"чехол", "5"},
        //                  new int[]{1, 2});
    }
}
