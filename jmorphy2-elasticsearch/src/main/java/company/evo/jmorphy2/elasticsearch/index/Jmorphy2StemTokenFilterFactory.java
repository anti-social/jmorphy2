package company.evo.jmorphy2.elasticsearch.index;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.lucene.Jmorphy2StemFilter;
import company.evo.jmorphy2.lucene.Jmorphy2StemFilterFactory;
import static company.evo.jmorphy2.lucene.Jmorphy2StemFilterFactory.parseTags;
import company.evo.jmorphy2.elasticsearch.indices.Jmorphy2Analysis;


public class Jmorphy2StemTokenFilterFactory extends AbstractTokenFilterFactory {
    private final MorphAnalyzer morph;

    private final List<Set<String>> includeTags;
    private final List<Set<String>> excludeTags;

    private static final String JMORPHY2_DICT_LOCATION_SETTING =
        "indices.analysis.jmorphy2.dictionary.location";
    private static final String DEFAULT_JMORPHY2_DICT_LOCATION = "jmorphy2";
    public static final int DEFAULT_CACHE_SIZE = 10000;

    public Jmorphy2StemTokenFilterFactory(IndexSettings indexSettings,
                                          Environment environment,
                                          String name,
                                          Settings settings) {
        super(indexSettings, name, settings);

        // morph = null;
        try {
            morph = getMorphAnalyzer(settings, environment);
        } catch (IOException exc) {
            throw new RuntimeException("Unable to load dictionary", exc);
        }
        includeTags = parseTags(settings.get("include_tags"));
        excludeTags = parseTags(settings.get("exclude_tags"));
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2StemFilter(tokenStream, morph, includeTags, excludeTags);
    }

    private MorphAnalyzer getMorphAnalyzer(Settings settings, Environment environment)
        throws IOException
    {
        String dictsLocation = settings.get(JMORPHY2_DICT_LOCATION_SETTING, null);
        String dictName = settings.get("name", "ru");
        if (dictName == null) {
            throw new ElasticsearchException("You must specify jmorphy dictionary name");
        }
        File dictPath = null;
        if (dictsLocation == null) {
            dictPath = new File(new File(
                    environment.configFile().toFile(), DEFAULT_JMORPHY2_DICT_LOCATION),
                dictName);
        } else {
            dictPath = new File(dictsLocation, dictName);
        }
        if (!dictPath.exists() || !dictPath.isDirectory()) {
            throw new ElasticsearchException(String.format(Locale.ROOT,
                "Could not find jmorphy2 dictionary [%s] by path [%s] - %s",
                dictName, dictPath, environment.configFile().toFile().getParentFile().list().length));
        }

        File pymorphy2DictsDir = new File(dictPath, "pymorphy2_dicts");

        File replacesFile = new File(dictPath, "replaces.json");
        Map<Character, String> replaces = null;
        if (replacesFile.exists() && replacesFile.isFile()) {
            replaces = Jmorphy2StemFilterFactory.parseReplaces(new FileInputStream(replacesFile));
        }

        int cacheSize = settings.getAsInt("cache_size", DEFAULT_CACHE_SIZE);

        return new MorphAnalyzer.Builder()
            .dictPath(pymorphy2DictsDir.getPath())
            .charSubstitutes(replaces)
            .cacheSize(cacheSize)
            .build();
    }
}
