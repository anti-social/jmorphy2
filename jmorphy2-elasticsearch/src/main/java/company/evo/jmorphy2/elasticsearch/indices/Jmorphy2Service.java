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

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.env.Environment;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.component.AbstractComponent;

import company.evo.jmorphy2.FileLoader;
import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.ResourceFileLoader;
import company.evo.jmorphy2.nlp.Ruleset;
import company.evo.jmorphy2.nlp.Tagger;
import company.evo.jmorphy2.nlp.Parser;
import company.evo.jmorphy2.nlp.SimpleTagger;
import company.evo.jmorphy2.nlp.SimpleParser;
import company.evo.jmorphy2.nlp.SubjectExtractor;
import company.evo.jmorphy2.lucene.Jmorphy2StemFilterFactory;


public class Jmorphy2Service extends AbstractComponent {
    public static final String JMORPHY2_DICT_LOCATION_SETTING =
        "indices.analysis.jmorphy2.dictionary.location";
    public static final String DEFAULT_JMORPHY2_DICT_LOCATION = "jmorphy2";
    public static final int DEFAULT_CACHE_SIZE = 10000;

    private final Path jmorphy2Dir;
    private final int defaultCacheSize;
    private final int taggerThreshold;
    private final int parserThreshold;

    private final Map<String, MorphAnalyzer> morphAnalyzers = new HashMap<>();
    private final Map<String, SubjectExtractor> subjectExtractors = new HashMap<>();

    public Jmorphy2Service(final Settings settings) throws IOException {
        super(settings);
        Environment env = new Environment(settings);
        this.jmorphy2Dir = resolveJmorphy2Directory(settings, env);
        this.defaultCacheSize = settings.getAsInt
            ("cache_size", DEFAULT_CACHE_SIZE);
        this.taggerThreshold = settings.getAsInt
            ("tagger_threshold", SimpleTagger.DEFAULT_THRESHOLD);
        this.parserThreshold = settings.getAsInt
            ("parser_threshold", SimpleParser.DEFAULT_THRESHOLD);
        scanAndLoad();
    }

    public MorphAnalyzer getMorphAnalyzer(String lang) {
        MorphAnalyzer morph = morphAnalyzers.get(lang);
        if (morph != null) {
            return morph;
        }
        // Try to load from resources
        // TODO: Make as jmorphy2-core api
        FileLoader loader = new ResourceFileLoader
            (String.format(Locale.ROOT, "/company/evo/jmorphy2/%s/pymorphy2_dicts", lang));
        try {
            InputStream metaStream = loader.newStream("meta.json");
            if (metaStream == null) {
                return null;
            }
            metaStream.close();
            Path replacesPath = jmorphy2Dir.resolve(lang).resolve("replaces.json");
            int cacheSize = settings.getAsInt("cache_size", DEFAULT_CACHE_SIZE);
            return new MorphAnalyzer.Builder()
                .fileLoader(loader)
                .charSubstitutes(parseReplaces(replacesPath))
                .cacheSize(cacheSize)
                .build();
        } catch (IOException e) {
            throw new IllegalStateException
                (String.format(Locale.ROOT, "Error when loading jmorphy2 dictionary: [%s]", lang), e);
        }
    }

    public SubjectExtractor getSubjectExtractor(String lang) {
        return subjectExtractors.get(lang);
    }

    private SubjectExtractor loadSubjectExtractor(Path path, MorphAnalyzer morph)
        throws IOException
    {
        // TODO: make builder api for subject extractor
        Path taggerRulesPath = path.resolve("tagger_rules.txt");
        Tagger tagger;
        if (Files.isRegularFile(taggerRulesPath)) {
            try (InputStream rulesStream = Files.newInputStream(taggerRulesPath)) {
                tagger = new SimpleTagger(morph,
                                          new Ruleset(rulesStream),
                                          taggerThreshold);
            }
        } else {
            tagger = new SimpleTagger(morph, taggerThreshold);
        }
        Path parserRulesPath = path.resolve("parser_rules.txt");
        Parser parser;
        if (Files.isRegularFile(parserRulesPath)) {
            try (InputStream rulesStream = Files.newInputStream(parserRulesPath)) {
                parser = new SimpleParser(morph,
                                          tagger,
                                          new Ruleset(rulesStream),
                                          parserThreshold);
            }
        } else {
            parser = new SimpleParser(morph, tagger, parserThreshold);
        }
        Path extractorRulesPath = path.resolve("extract_rules.txt");
        String extractorRules;
        if (Files.isRegularFile(extractorRulesPath)) {
            extractorRules = new String(Files.readAllBytes(extractorRulesPath),
                                        StandardCharsets.UTF_8);
        } else {
            extractorRules = "+NP,nomn +NP,accs -PP -Geox NOUN,nomn NOUN,accs LATN NUMB";
        }
        return new SubjectExtractor(parser, extractorRules, true);
    }

    private Path resolveJmorphy2Directory(Settings settings, Environment env) {
        Path configDir = env.configFile();
        String dictsLocation = settings.get(JMORPHY2_DICT_LOCATION_SETTING, null);
        if (dictsLocation != null) {
            return configDir.resolve(dictsLocation);
        }
        return configDir.resolve(DEFAULT_JMORPHY2_DICT_LOCATION);
    }

    private MorphAnalyzer loadMorphAnalyzer(Path path)
        throws IOException
    {
        Path pymorphy2DictsDir = path.resolve("pymorphy2_dicts");
        int cacheSize = settings.getAsInt("cache_size", DEFAULT_CACHE_SIZE);
        return new MorphAnalyzer.Builder()
            .dictPath(pymorphy2DictsDir.toString())
            .charSubstitutes(parseReplaces(path.resolve("replaces.json")))
            .cacheSize(cacheSize)
            .build();
    }

    private Map<Character, String> parseReplaces(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            return Jmorphy2StemFilterFactory
                .parseReplaces(Files.newInputStream(path));
        }
        return null;
    }

    private void scanAndLoad() throws IOException {
        // Scan dictionaries inside config directory
        if (Files.isDirectory(jmorphy2Dir)) {
            try (DirectoryStream<Path> dir = Files.newDirectoryStream(jmorphy2Dir)) {
                for (Path path : dir) {
                    if (Files.isDirectory(path)) {
                        Path pymorphy2DictsDir = path.resolve("pymorphy2_dicts");
                        if (Files.isDirectory(pymorphy2DictsDir)) {
                            String lang = path.getFileName().toString();
                            MorphAnalyzer morph = loadMorphAnalyzer(path);
                            morphAnalyzers.put(lang, morph);
                            subjectExtractors.put(lang, loadSubjectExtractor(path, morph));
                        }
                    }
                }
            }
        }
    }
}
