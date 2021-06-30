package company.evo.jmorphy2.lucene;

import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import company.evo.jmorphy2.JSONUtils;
import company.evo.jmorphy2.MorphAnalyzer;

// TODO: Move factories into jmorphy2-solr
public class Jmorphy2StemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    public static final String DICT_PATH_ATTR = "dict";
    public static final String REPLACES_PATH_ATTR = "replaces";
    // public static final String CACHE_SIZE_ATTR = "cacheSize";
    public static final String EXCLUDE_TAGS_ATTR = "excludeTags";
    public static final String INCLUDE_TAGS_ATTR = "includeTags";
    public static final String ENABLE_POSITION_INCREMENTS_ATTR = "enablePositionIncrements";

    public static final String DEFAULT_DICT_PATH = "pymorphy2_dicts";
    // public static final int DEFAULT_CACHE_SIZE = 10000;

    private MorphAnalyzer morph;
    private final String dictPath;
    private final String replacesPath;
    private final List<Set<String>> includeTags;
    private final List<Set<String>> excludeTags;
    private final boolean enablePositionIncrements;

    public Jmorphy2StemFilterFactory(Map<String,String> args) {
        super(args);

        String dictPath = args.get(DICT_PATH_ATTR);
        if (dictPath == null) {
            dictPath = DEFAULT_DICT_PATH;
        }

        this.dictPath = dictPath;
        this.replacesPath = args.get(REPLACES_PATH_ATTR);
        this.excludeTags = parseTags(args.get(EXCLUDE_TAGS_ATTR));
        this.includeTags = parseTags(args.get(INCLUDE_TAGS_ATTR));
        this.enablePositionIncrements = getBoolean(args, ENABLE_POSITION_INCREMENTS_ATTR, true);
    }

    public void inform(ResourceLoader loader) throws IOException {
        Map<Character,String> replaceChars = null;
        if (replacesPath != null) {
            replaceChars = parseReplaces(loader.openResource(replacesPath));
        }

        morph = new MorphAnalyzer.Builder<>()
            .fileLoader(new LuceneFileLoader(loader, dictPath))
            .charSubstitutes(replaceChars)
            .build();
    }

    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2StemFilter(tokenStream,
                                      morph,
                                      includeTags,
                                      excludeTags,
                                      enablePositionIncrements);
    }

    public static List<Set<String>> parseTags(String tagsStr) {
        List<Set<String>> parsedTags = null;
        if (tagsStr != null) {
            parsedTags = new ArrayList<>();
            for (String tagStr : tagsStr.split(" ")) {
                Set<String> grammemeValues = new HashSet<>();
                parsedTags.add(grammemeValues);
                Collections.addAll(grammemeValues, tagStr.split(","));
            }
        }

        return parsedTags;
    }

    @SuppressWarnings("unchecked")
    public static Map<Character,String> parseReplaces(InputStream stream) throws IOException {
        Map<Character,String> replaceChars = new HashMap<>();
        for (Map.Entry<String,String> entry : ((Map<String,String>) JSONUtils.parseJSON(stream)).entrySet()) {
            String c = entry.getKey();
            if (c.length() != 1) {
                throw new IOException(String.format("Replaceable string must contain only one character: '%s'", c));
            }

            replaceChars.put(c.charAt(0), entry.getValue());
        }
        return replaceChars;
    }
}
