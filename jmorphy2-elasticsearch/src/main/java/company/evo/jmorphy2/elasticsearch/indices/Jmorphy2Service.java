package company.evo.jmorphy2.elasticsearch.indices;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
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

import company.evo.jmorphy2.MorphAnalyzer;
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
        this.jmorphy2Dir = resolveJmorphy2Directory(settings);
        this.defaultCacheSize = settings.getAsInt
            ("cache_size", DEFAULT_CACHE_SIZE);
        this.taggerThreshold = settings.getAsInt
            ("tagger_threshold", SimpleTagger.DEFAULT_THRESHOLD);
        this.parserThreshold = settings.getAsInt
            ("parser_threshold", SimpleParser.DEFAULT_THRESHOLD);
        scanAndLoad();
    }

    public MorphAnalyzer getMorphAnalyzer(String lang) {
        return morphAnalyzers.get(lang);
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
            InputStream rulesStream = Files.newInputStream(taggerRulesPath);
            tagger = new SimpleTagger(morph,
                                      new Ruleset(rulesStream),
                                      taggerThreshold);
            rulesStream.close();
        } else {
            tagger = new SimpleTagger(morph, taggerThreshold);
        }
        Path parserRulesPath = path.resolve("parser_rules.txt");
        Parser parser;
        if (Files.isRegularFile(parserRulesPath)) {
            InputStream rulesStream = Files.newInputStream(parserRulesPath);
            parser = new SimpleParser(morph,
                                      tagger,
                                      new Ruleset(rulesStream),
                                      parserThreshold);
            rulesStream.close();
        } else {
            parser = new SimpleParser(morph, tagger, parserThreshold);
        }
        Path extractorRulesPath = path.resolve("extract_rules.txt");
        String extractorRules;
        if (Files.isRegularFile(extractorRulesPath)) {
            extractorRules = new String(Files.readAllBytes(extractorRulesPath));
        } else {
            extractorRules = "+NP,nomn +NP,accs -PP -Geox NOUN,nomn NOUN,accs LATN NUMB";
        }
        return new SubjectExtractor(parser, extractorRules, true);
    }

    private Path resolveJmorphy2Directory(Settings settings) {
        String dictsLocation = settings.get(JMORPHY2_DICT_LOCATION_SETTING, null);
        if (dictsLocation != null) {
            return PathUtils.get(dictsLocation);
        }
        return PathUtils.get(settings.get(Environment.PATH_CONF_SETTING.getKey()))
            .resolve(DEFAULT_JMORPHY2_DICT_LOCATION);
    }

    private MorphAnalyzer loadMorphAnalyzer(Path path)
        throws IOException
    {
        Path pymorphy2DictsDir = path.resolve("pymorphy2_dicts");
        Path replacesFile = path.resolve("replaces.json");
        Map<Character, String> replaces = null;
        if (Files.isRegularFile(replacesFile)) {
            replaces = Jmorphy2StemFilterFactory
                .parseReplaces(Files.newInputStream(replacesFile));
        }

        int cacheSize = settings.getAsInt("cache_size", DEFAULT_CACHE_SIZE);

        return new MorphAnalyzer.Builder()
            .dictPath(pymorphy2DictsDir.toString())
            .charSubstitutes(replaces)
            .cacheSize(cacheSize)
            .build();
    }

    private void scanAndLoad() throws IOException {
        if (Files.isDirectory(jmorphy2Dir)) {
            DirectoryStream<Path> dir = Files.newDirectoryStream(jmorphy2Dir);
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
            dir.close();
        }
    }
}
