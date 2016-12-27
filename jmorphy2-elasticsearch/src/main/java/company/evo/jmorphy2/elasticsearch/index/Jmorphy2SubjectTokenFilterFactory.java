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

import java.util.Set;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.analysis.TokenStream;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.nlp.SubjectExtractor;
import company.evo.jmorphy2.lucene.Jmorphy2SubjectFilter;
import company.evo.jmorphy2.elasticsearch.indices.Jmorphy2Service;


public class Jmorphy2SubjectTokenFilterFactory extends AbstractTokenFilterFactory {
    private final SubjectExtractor subjExtractor;
    private final int maxSentenceLength;

    public Jmorphy2SubjectTokenFilterFactory(IndexSettings indexSettings,
                                             Environment environment,
                                             String name,
                                             Settings settings,
                                             Jmorphy2Service jmorphy2Service) {
        super(indexSettings, name, settings);

        String lang = settings.get("name");
        if (lang == null) {
            throw new IllegalArgumentException
                ("Missing [lang] configuration for jmorphy2 subject token filter");
        }
        subjExtractor = jmorphy2Service.getSubjectExtractor(lang);
        if (subjExtractor == null) {
            throw new IllegalArgumentException
                (String.format(Locale.ROOT, "Cannot find subject extractor for lang: %s", lang));
        }

        maxSentenceLength = settings.getAsInt("max_sentence_length", 10);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2SubjectFilter(tokenStream, subjExtractor, maxSentenceLength);
    }
}
