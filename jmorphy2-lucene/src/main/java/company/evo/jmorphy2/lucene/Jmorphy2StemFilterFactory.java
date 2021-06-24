package company.evo.jmorphy2.lucene;

import java.io.InputStream;
import java.io.IOException;
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


public class Jmorphy2StemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    public static final String DICT_PATH_ATTR = "dict";
    public static final String REPLACES_PATH_ATTR = "replaces";
    public static final String CACHE_SIZE_ATTR = "cacheSize";
    public static final String EXCLUDE_TAGS_ATTR = "excludeTags";
    public static final String INCLUDE_TAGS_ATTR = "includeTags";
    public static final String ENABLE_POSITION_INCREMENTS_ATTR = "enablePositionIncrements";

    public static final String DEFAULT_DICT_PATH = "pymorphy2_dicts";
    public static final int DEFAULT_CACHE_SIZE = 10000;

    private MorphAnalyzer morph;
    private final String dictPath;
    private final String replacesPath;
    private final int cacheSize;
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
        this.cacheSize = getInt(args, CACHE_SIZE_ATTR, DEFAULT_CACHE_SIZE);
        this.excludeTags = parseTags(args.get(EXCLUDE_TAGS_ATTR));
        this.includeTags = parseTags(args.get(INCLUDE_TAGS_ATTR));
        this.enablePositionIncrements = getBoolean(args, ENABLE_POSITION_INCREMENTS_ATTR, true);
    }

    public void inform(ResourceLoader loader) throws IOException {
        Map<Character,String> replaceChars = null;
        if (replacesPath != null) {
            replaceChars = parseReplaces(loader.openResource(replacesPath));
        }

        morph = new MorphAnalyzer.Builder()
            .fileLoader(new LuceneFileLoader(loader, dictPath))
            .charSubstitutes(replaceChars)
            .cacheSize(cacheSize)
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
            parsedTags = new ArrayList<Set<String>>();
            for (String tagStr : tagsStr.split(" ")) {
                Set<String> grammemeValues = new HashSet<String>();
                parsedTags.add(grammemeValues);
                for (String grammemeStr : tagStr.split(",")) {
                    grammemeValues.add(grammemeStr);
                }
            }
        }

        return parsedTags;
    }

    @SuppressWarnings("unchecked")
    public static Map<Character,String> parseReplaces(InputStream stream) throws IOException {
        Map<Character,String> replaceChars = new HashMap<Character,String>();
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
