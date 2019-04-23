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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;

import company.evo.jmorphy2.lucene.Jmorphy2Analyzer;


public class Jmorphy2AnalyzerProvider extends AbstractIndexAnalyzerProvider<Jmorphy2Analyzer> {

    private final Jmorphy2Analyzer analyzer;

    public Jmorphy2AnalyzerProvider(IndexSettings indexSettings,
                                    Environment environment,
                                    String name,
                                    Settings settings) {
        super(indexSettings, name, settings);
        // analyzer = new Jmorphy2Analyzer(version);
        analyzer = null;
    }


    @Override
    public Jmorphy2Analyzer get() {
        return analyzer;
    }
}
