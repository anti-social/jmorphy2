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

package company.evo.jmorphy2.elasticsearch.plugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.env.Environment;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

import company.evo.jmorphy2.elasticsearch.index.Jmorphy2StemTokenFilterFactory;
import company.evo.jmorphy2.elasticsearch.index.Jmorphy2SubjectTokenFilterFactory;
import company.evo.jmorphy2.elasticsearch.indices.Jmorphy2Service;


public class AnalysisJmorphy2Plugin extends Plugin implements AnalysisPlugin {
    private final Jmorphy2Service jmorphy2Service;

    public AnalysisJmorphy2Plugin(Settings settings, Path configPath) {
        super();
        Environment env = new Environment(settings, configPath);
        jmorphy2Service = new Jmorphy2Service(settings, env);
    }

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        Map<String, AnalysisProvider<TokenFilterFactory>> tokenFilters = new HashMap<>();
        tokenFilters.put("jmorphy2_stemmer",
            (Jmorphy2AnalysisProvider) (indexSettings, environment, name, settings) ->
                new Jmorphy2StemTokenFilterFactory
                    (indexSettings, environment, name, settings, jmorphy2Service)
        );
        tokenFilters.put("jmorphy2_subject",
            (Jmorphy2AnalysisProvider) (indexSettings, environment, name, settings) ->
                new Jmorphy2SubjectTokenFilterFactory
                    (indexSettings, environment, name, settings, jmorphy2Service)
        );
        return tokenFilters;
    }

    // public Map<String, AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
    //     return Collections.singletonMap("jmorphy2_analyzer", Jmorphy2AnalyzerProvider::new);
    // }

    public interface Jmorphy2AnalysisProvider extends AnalysisProvider<TokenFilterFactory> {
        @Override
        default boolean requiresAnalysisSettings() {
            return true;
        }
    }
}
