/*
 * Copyright 2016 Alexander Koval
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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


public class Jmorphy2StemTokenFilterFactoryTests extends ESTestCase {

    public void testJmorphy2StemTokenFilter() throws IOException {
        Path home = createTempDir();
        Settings settings = Settings.builder()
            .put(Environment.PATH_HOME_SETTING.getKey(), home.toString())
            .put(Environment.PATH_CONF_SETTING.getKey(), home.resolve("config"))
            .put("index.analysis.filter.jmorphy2.type", "jmorphy2_stemmer")
            .put("index.analysis.filter.jmorphy2.name", "ru")
            .put("index.analysis.filter.jmorphy2.exclude_tags", "NPRO PREP CONJ PRCL INTJ")
            .put("index.analysis.analyzer.text.tokenizer", "standard")
            .put("index.analysis.analyzer.text.filter", "jmorphy2")
            .build();

        copyFilesFromResources(settings, "ru");

        AnalysisJmorphy2Plugin plugin = new AnalysisJmorphy2Plugin(settings);
        TestAnalysis analysis = createTestAnalysis
            (new Index("test", "_na_"), settings, plugin);
        assertThat(analysis.tokenFilter.get("jmorphy2"),
                   instanceOf(Jmorphy2StemTokenFilterFactory.class));

        Analyzer analyzer = analysis.indexAnalyzers.get("text").analyzer();
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
