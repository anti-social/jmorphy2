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

import java.util.List;
import java.util.Locale;
import java.util.Set;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.elasticsearch.indices.Jmorphy2Service;
import company.evo.jmorphy2.lucene.Jmorphy2StemFilter;

import org.apache.lucene.analysis.TokenStream;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import static company.evo.jmorphy2.lucene.Jmorphy2StemFilterFactory.parseTags;


public class Jmorphy2StemTokenFilterFactory extends AbstractTokenFilterFactory {
    public static final int DEFAULT_CACHE_SIZE = 10000;

    private final MorphAnalyzer morph;

    private final List<Set<String>> includeTags;
    private final List<Set<String>> excludeTags;

    public Jmorphy2StemTokenFilterFactory(IndexSettings indexSettings,
                                          Environment environment,
                                          String name,
                                          Settings settings,
                                          Jmorphy2Service jmorphy2Service) {
        super(indexSettings, name, settings);

        String lang = settings.get("lang", settings.get("name"));
        String substitutesPath = settings.get("char_substitutes_path");
        int cacheSize = settings.getAsInt("cache_size", DEFAULT_CACHE_SIZE);
        if (lang == null) {
            throw new IllegalArgumentException
                ("Missing [lang] configuration for jmorphy2 token filter");
        }
        morph = jmorphy2Service.getMorphAnalyzer(lang, substitutesPath, cacheSize);
        if (morph == null) {
            throw new IllegalArgumentException
                (String.format(Locale.ROOT, "Cannot find dictionary for lang: [%s]", lang));
        }
        includeTags = parseTags(settings.get("include_tags"));
        excludeTags = parseTags(settings.get("exclude_tags"));
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2StemFilter(tokenStream, morph, includeTags, excludeTags);
    }
}
