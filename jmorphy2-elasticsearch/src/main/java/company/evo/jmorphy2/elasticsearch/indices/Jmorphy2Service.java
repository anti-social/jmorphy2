package company.evo.jmorphy2.elasticsearch.indices;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.env.Environment;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.component.AbstractComponent;
// import org.elasticsearch.indices.analysis.IndicesAnalysisService;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.nlp.Ruleset;
import company.evo.jmorphy2.nlp.Tagger;
import company.evo.jmorphy2.nlp.Parser;
import company.evo.jmorphy2.nlp.SimpleTagger;
import company.evo.jmorphy2.nlp.SimpleParser;
import company.evo.jmorphy2.nlp.SubjectExtractor;
import company.evo.jmorphy2.lucene.Jmorphy2StemFilterFactory;


public class Jmorphy2Service extends AbstractComponent {
    private static final String JMORPHY2_DICT_LOCATION_SETTING =
        "indices.analysis.jmorphy2.dictionary.location";
    private static final String DEFAULT_JMORPHY2_DICT_LOCATION = "jmorphy2";
    public static final int DEFAULT_CACHE_SIZE = 10000;

    private final Path jmorphy2Dir;
    private final int defaultCacheSize;

    private final Map<String, MorphAnalyzer> morphAnalyzers = new HashMap<>();
    private final Map<String, SubjectExtractor> subjectExtractors = new HashMap<>();

    public Jmorphy2Service(final Settings settings) throws IOException {
        super(settings);
        this.jmorphy2Dir = resolveJmorphy2Directory(settings);
        this.defaultCacheSize = settings.getAsInt("indices.analysis.jmorphy2.dictionary.cache_size",
                                                  DEFAULT_CACHE_SIZE);
        // this.morphAnalyzers =
        //     CacheBuilder.newBuilder()
        //     .build(new CacheLoader<String, MorphAnalyzer>() {
        //         @Override
        //         public MorphAnalyzer load(String name) throws Exception {
        //             return loadMorphAnalyzer(name, settings, env);
        //         }
        //     });
        scanAndLoadMorphAnalyzers();
        // throw new RuntimeException(jmorphy2Dir.toString());

        // this.subjectExtractors =
        //     CacheBuilder.newBuilder()
        //     .build(new CacheLoader<String, SubjectExtractor>() {
        //         @Override
        //         public SubjectExtractor load(String name) throws Exception {
        //             return loadSubjectExtractor(name, settings, env);
        //         }
        //     });
    }

    public MorphAnalyzer getMorphAnalyzer(String name) {
        return morphAnalyzers.get(name);
    }

    // public SubjectExtractor getSubjectExtractor(String name) {
    //     return subjectExtractors.getUnchecked(name);
    // }

    // private SubjectExtractor loadSubjectExtractor(String name, Settings settings, Environment env) throws IOException {
    //     MorphAnalyzer morph = getMorphAnalyzer(name);
    //     File dicDir = new File(jmorphy2Dir, name);
    //     File taggerRulesFile = new File(dicDir, "tagger_rules.txt");
    //     Tagger tagger =
    //         new SimpleTagger(morph,
    //                          new Ruleset(new FileInputStream(taggerRulesFile)),
    //                          settings.getAsInt("tagger_threshold", SimpleTagger.DEFAULT_THRESHOLD));
    //     File parserRulesFile = new File(dicDir, "parser_rules.txt");
    //     Parser parser =
    //         new SimpleParser(morph,
    //                          tagger,
    //                          new Ruleset(new FileInputStream(parserRulesFile)),
    //                          settings.getAsInt("parser_threshold", SimpleParser.DEFAULT_THRESHOLD));

    //     Path extractionRulesPath = Paths.get(dicDir.getPath(), "extract_rules.txt");
    //     String extractionRules = new String(Files.readAllBytes(extractionRulesPath));
    //     return new SubjectExtractor(parser, extractionRules, true);
    // }

    private Path resolveJmorphy2Directory(Settings settings) {
        System.out.println(settings.get(Environment.PATH_CONF_SETTING.getKey()));
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


    private void scanAndLoadMorphAnalyzers() throws IOException {
        if (!Files.isDirectory(jmorphy2Dir)) {
            throw new RuntimeException(jmorphy2Dir.toString());
        }
        if (Files.isDirectory(jmorphy2Dir)) {
            DirectoryStream<Path> dir = Files.newDirectoryStream(jmorphy2Dir);
            for (Path path : dir) {
                if (Files.isDirectory(path)) {
                    Path pymorphy2DictsDir = path.resolve("pymorphy2_dicts");
                    if (Files.isDirectory(pymorphy2DictsDir)) {
                        String lang = path.getFileName().toString();
                        morphAnalyzers.put(lang, loadMorphAnalyzer(path));
                    }
                }
            }
            dir.close();
        }
    }
}
