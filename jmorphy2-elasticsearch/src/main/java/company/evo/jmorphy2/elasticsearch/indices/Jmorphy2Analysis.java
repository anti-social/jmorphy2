package company.evo.jmorphy2.elasticsearch.indices;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Map;
import java.util.Locale;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.env.Environment;
import org.elasticsearch.common.inject.Inject;
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


public class Jmorphy2Analysis extends AbstractComponent {
    public static final int DEFAULT_CACHE_SIZE = 10000;

    private final File jmorphy2Dir;
    private final int defaultCacheSize;

    private final LoadingCache<String, MorphAnalyzer> morphAnalyzers;
    private final LoadingCache<String, SubjectExtractor> subjectExtractors;

    @Inject
    public Jmorphy2Analysis(final Settings settings, final Environment env) {
        super(settings);
        this.jmorphy2Dir = resolveJmorphy2Directory(settings, env);
        this.defaultCacheSize = settings.getAsInt("indices.analysis.jmorphy2.dictionary.cache_size",
                                                  DEFAULT_CACHE_SIZE);
        this.morphAnalyzers =
            CacheBuilder.newBuilder()
            .build(new CacheLoader<String, MorphAnalyzer>() {
                @Override
                public MorphAnalyzer load(String name) throws Exception {
                    return loadMorphAnalyzer(name, settings, env);
                }
            });
        scanAndLoadMorphAnalyzers();

        this.subjectExtractors =
            CacheBuilder.newBuilder()
            .build(new CacheLoader<String, SubjectExtractor>() {
                @Override
                public SubjectExtractor load(String name) throws Exception {
                    return loadSubjectExtractor(name, settings, env);
                }
            });
    }

    public MorphAnalyzer getMorphAnalyzer(String name) {
        return morphAnalyzers.getUnchecked(name);
    }

    public SubjectExtractor getSubjectExtractor(String name) {
        return subjectExtractors.getUnchecked(name);
    }

    private SubjectExtractor loadSubjectExtractor(String name, Settings settings, Environment env) throws IOException {
        MorphAnalyzer morph = getMorphAnalyzer(name);
        File dicDir = new File(jmorphy2Dir, name);
        File taggerRulesFile = new File(dicDir, "tagger_rules.txt");
        Tagger tagger =
            new SimpleTagger(morph,
                             new Ruleset(new FileInputStream(taggerRulesFile)),
                             settings.getAsInt("tagger_threshold", SimpleTagger.DEFAULT_THRESHOLD));
        File parserRulesFile = new File(dicDir, "parser_rules.txt");
        Parser parser =
            new SimpleParser(morph,
                             tagger,
                             new Ruleset(new FileInputStream(parserRulesFile)),
                             settings.getAsInt("parser_threshold", SimpleParser.DEFAULT_THRESHOLD));

        Path extractionRulesPath = Paths.get(dicDir.getPath(), "extract_rules.txt");
        String extractionRules = new String(Files.readAllBytes(extractionRulesPath));
        return new SubjectExtractor(parser, extractionRules, true);
    }

    private File resolveJmorphy2Directory(Settings settings, Environment env) {
        String location = settings.get("indices.analysis.jmorphy2.dictionary.location", null);
        if (location != null) {
            return new File(location);
        }
        return new File(env.configFile().toFile(), "jmorphy2");
    }

    private MorphAnalyzer loadMorphAnalyzer(String name, Settings settings, Environment env)
        throws IOException
    {
        File dicDir = new File(jmorphy2Dir, name);
        if (!dicDir.exists() || !dicDir.isDirectory()) {
            throw new ElasticsearchException(String.format(Locale.ROOT, "Could not find jmorphy2 dictionary [%s]", name));
        }

        File pymorphy2DictsDir = new File(dicDir, "pymorphy2_dicts");

        File replacesFile = new File(dicDir, "replaces.json");
        Map<Character, String> replaces = null;
        if (replacesFile.exists() && replacesFile.isFile()) {
            replaces = Jmorphy2StemFilterFactory.parseReplaces(new FileInputStream(replacesFile));
        }

        int cacheSize = defaultCacheSize;

        return new MorphAnalyzer.Builder()
            .dictPath(pymorphy2DictsDir.getPath())
            .charSubstitutes(replaces)
            .cacheSize(cacheSize)
            .build();
    }

    private void scanAndLoadMorphAnalyzers() {
        if (jmorphy2Dir.exists() && jmorphy2Dir.isDirectory()) {
            for (File file : jmorphy2Dir.listFiles()) {
                if (file.isDirectory()) {
                    File pymorphy2DictsDir = new File(file, "pymorphy2_dicts");
                    if (pymorphy2DictsDir.exists() && pymorphy2DictsDir.isDirectory()) {
                        morphAnalyzers.getUnchecked(file.getName());
                    }
                }
            }
        }
    }
}
