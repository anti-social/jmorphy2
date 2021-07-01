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

package company.evo.jmorphy2.elasticsearch.indices;

import company.evo.jmorphy2.FileLoader;
import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.ResourceFileLoader;
import company.evo.jmorphy2.lucene.Jmorphy2StemFilterFactory;
import company.evo.jmorphy2.nlp.Parser;
import company.evo.jmorphy2.nlp.Ruleset;
import company.evo.jmorphy2.nlp.SimpleParser;
import company.evo.jmorphy2.nlp.SimpleTagger;
import company.evo.jmorphy2.nlp.SubjectExtractor;
import company.evo.jmorphy2.nlp.Tagger;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class Jmorphy2Service {
    private static final String JMORPHY2_DICT_LOCATION_SETTING =
        "indices.analysis.jmorphy2.dictionary.location";
    private static final String DEFAULT_JMORPHY2_DICT_LOCATION = "jmorphy2";

    private final Environment env;

    private final Path jmorphy2Dir;

    private final Map<MorphAnalyzerCacheKey, MorphAnalyzer> morphAnalyzers = new ConcurrentHashMap<>();
    private final Map<SubjectExtractorCacheKey, SubjectExtractor> subjectExtractors = new ConcurrentHashMap<>();

    public Jmorphy2Service(final Settings settings, final Environment env) {
        this.env = env;
        this.jmorphy2Dir = resolveJmorphy2Directory(settings, env);
    }

    public MorphAnalyzer getMorphAnalyzer(String lang, String substitutesPath, Integer cacheSize) {
        MorphAnalyzerCacheKey key = new MorphAnalyzerCacheKey(lang, substitutesPath, cacheSize);
        return morphAnalyzers.computeIfAbsent(key, this::loadMorphAnalyzer);
    }

    public SubjectExtractor getSubjectExtractor
        (String lang, String substitutesPath, int analyzerCacheSize,
         String taggerRulesPath, String parserRulesPath, String extractorRulesPath,
         int taggerThreshold, int parserThreshold)
    {
        SubjectExtractorCacheKey key = new SubjectExtractorCacheKey
            (lang, substitutesPath, analyzerCacheSize,
             taggerRulesPath, parserRulesPath, extractorRulesPath,
             taggerThreshold, parserThreshold);
        return subjectExtractors.computeIfAbsent(key, this::loadSubjectExtractor);
    }

    private MorphAnalyzer loadMorphAnalyzer(MorphAnalyzerCacheKey key) {
        try {
            return loadMorphAnalyzerFromFilesystem(key)
                .orElse(loadMorphAnalyzerFromResources(key).orElse(null));
        } catch (IOException e) {
            throw new IllegalStateException(
                String.format(Locale.ROOT, "Error when loading jmorphy2 dictionary: [%s]", key.lang), e
            );
        }
    }

    private Optional<MorphAnalyzer> loadMorphAnalyzerFromFilesystem(MorphAnalyzerCacheKey key)
        throws IOException
    {
        Path dictsPath = jmorphy2Dir.resolve(key.lang).resolve("pymorphy2_dicts");
        if (Files.isDirectory(dictsPath) && Files.isRegularFile(dictsPath.resolve("meta.json"))) {
            var morphBuilder = new CachingMorphAnalyzer.Builder()
                .cacheSize(key.cacheSize)
                .dictPath(dictsPath.toString());
            if (key.substitutesPath != null) {
                Path substitutesPath = env.configFile().resolve(key.substitutesPath);
                morphBuilder.charSubstitutes(parseSubstitutes(substitutesPath));
            }

            return Optional.of(morphBuilder.build());
        }
        return Optional.empty();
    }

    private Optional<MorphAnalyzer> loadMorphAnalyzerFromResources(MorphAnalyzerCacheKey key)
        throws IOException
    {
        // TODO: Make as jmorphy2-core api
        FileLoader loader = new ResourceFileLoader
            (String.format(Locale.ROOT, "/company/evo/jmorphy2/%s/pymorphy2_dicts", key.lang));
        try (InputStream metaStream = loader.newStream("meta.json")) {
            if (metaStream == null) {
                return Optional.empty();
            }
        }
        var morphBuilder = new CachingMorphAnalyzer.Builder()
            .cacheSize(key.cacheSize)
            .fileLoader(loader);
        if (key.substitutesPath != null) {
            Path substitutesPath = env.configFile().resolve(key.substitutesPath);
            morphBuilder.charSubstitutes(parseSubstitutes(substitutesPath));
        }

        return Optional.of(morphBuilder.build());
    }

    private SubjectExtractor loadSubjectExtractor(SubjectExtractorCacheKey key)
    {
        // TODO: make builder api for subject extractor
        try {
            var morph = getMorphAnalyzer(key.lang, key.substitutesPath, key.analyzerCacheSize);
            Tagger tagger;
            if (key.taggerRulesPath != null) {
                try (InputStream rulesStream =
                             Files.newInputStream(env.configFile().resolve(key.taggerRulesPath))) {
                    tagger = new SimpleTagger(morph,
                                              new Ruleset(rulesStream),
                                              key.taggerThreshold);
                }
            } else {
                tagger = new SimpleTagger(morph, key.taggerThreshold);
            }
            Parser parser;
            if (key.parserRulesPath != null) {
                try (InputStream rulesStream =
                     Files.newInputStream(env.configFile().resolve(key.parserRulesPath))) {
                    parser = new SimpleParser(morph,
                                              tagger,
                                              new Ruleset(rulesStream),
                                              key.parserThreshold);
                }
            } else {
                parser = new SimpleParser(morph, tagger, key.parserThreshold);
            }
            String extractorRules;
            if (key.extractorRulesPath != null) {
                extractorRules = Files.readString(env.configFile().resolve(key.extractorRulesPath));
            } else {
                extractorRules = "+NP,nomn +NP,accs -PP -Geox NOUN,nomn NOUN,accs LATN NUMB";
            }
            return new SubjectExtractor(parser, extractorRules, true);
        } catch (IOException e) {
            throw new IllegalStateException
                (String.format(Locale.ROOT, "Error when loading subject extractor for lang: [%s]", key.lang), e);
        }
    }

    private Path resolveJmorphy2Directory(Settings settings, Environment env) {
        Path configDir = env.configFile();
        String dictsLocation = settings.get(JMORPHY2_DICT_LOCATION_SETTING, null);
        if (dictsLocation != null) {
            return configDir.resolve(dictsLocation);
        }
        return configDir.resolve(DEFAULT_JMORPHY2_DICT_LOCATION);
    }

    private Map<Character, String> parseSubstitutes(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            return Jmorphy2StemFilterFactory
                .parseReplaces(Files.newInputStream(path));
        }
        return null;
    }

    private static class MorphAnalyzerCacheKey {
        private final String lang;
        private final String substitutesPath;
        private final int cacheSize;

        MorphAnalyzerCacheKey(String lang, String substitutesPath, int cacheSize) {
            this.lang = lang;
            this.substitutesPath = substitutesPath;
            this.cacheSize = cacheSize;
        }

        @Override
        public int hashCode() {
            final int p = 31;
            int h = lang.hashCode();
            if (substitutesPath != null) {
                h = p * h + substitutesPath.hashCode();
            }
            h = p * h + cacheSize;
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass()) {
                return false;
            }
            MorphAnalyzerCacheKey other = (MorphAnalyzerCacheKey) obj;
            return lang.equals(other.lang)
                && (Objects.equals(substitutesPath, other.substitutesPath))
                && cacheSize == other.cacheSize;
        }
    }

    private static class SubjectExtractorCacheKey {
        private final String lang;
        private final String substitutesPath;
        private final int analyzerCacheSize;
        private final String taggerRulesPath;
        private final String parserRulesPath;
        private final String extractorRulesPath;
        private final int taggerThreshold;
        private final int parserThreshold;

        SubjectExtractorCacheKey
            (String lang, String substitutesPath, int analyzerCacheSize,
             String taggerRulesPath, String parserRulesPath, String extractorRulesPath,
             int taggerThreshold, int parserThreshold)
        {
            this.lang = lang;
            this.substitutesPath = substitutesPath;
            this.analyzerCacheSize = analyzerCacheSize;
            this.taggerRulesPath = taggerRulesPath;
            this.parserRulesPath = parserRulesPath;
            this.extractorRulesPath = extractorRulesPath;
            this.taggerThreshold = taggerThreshold;
            this.parserThreshold = parserThreshold;
        }

        @Override
        public int hashCode() {
            final int p = 31;
            int h = lang.hashCode();
            if (substitutesPath != null) {
                h = p * h + substitutesPath.hashCode();
            }
            h = p * h + analyzerCacheSize;
            if (taggerRulesPath != null) {
                h = p * h + taggerRulesPath.hashCode();
            }
            if (parserRulesPath != null) {
                h = p * h + parserRulesPath.hashCode();
            }
            if (extractorRulesPath != null) {
                h = p * h + extractorRulesPath.hashCode();
            }
            h = p * h + taggerThreshold;
            h = p * h + parserThreshold;
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass()) {
                return false;
            }
            SubjectExtractorCacheKey other = (SubjectExtractorCacheKey) obj;
            return lang.equals(other.lang)
                && (Objects.equals(substitutesPath, other.substitutesPath))
                && analyzerCacheSize == other.analyzerCacheSize
                && (Objects.equals(taggerRulesPath, other.taggerRulesPath))
                && (Objects.equals(parserRulesPath, other.parserRulesPath))
                && (Objects.equals(extractorRulesPath, other.extractorRulesPath))
                && taggerThreshold == other.taggerThreshold
                && parserThreshold == other.parserThreshold;
        }
    }
}
