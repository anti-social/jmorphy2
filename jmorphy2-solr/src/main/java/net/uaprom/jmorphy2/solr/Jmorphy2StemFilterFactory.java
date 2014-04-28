package net.uaprom.jmorphy2.solr;

import java.io.File;
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

import net.uaprom.jmorphy2.JSONUtils;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class Jmorphy2StemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    public static final String DICT_PATH_ATTR = "dict";
    public static final String REPLACES_PATH_ATTR = "replaces";
    public static final String INCLUDE_TAGS_ATTR = "includeTags";
    public static final String INCLUDE_UNKNOWN_ATTR = "includeUnknown";
    
    public static final String DEFAULT_DICT_PATH = "pymorphy2_dicts";

    private MorphAnalyzer morph;
    private final String dictPath;
    private final String replacesPath;
    private final List<Set<String>> includeTags;
    private final boolean includeUnknown;

    public Jmorphy2StemFilterFactory(Map<String,String> args) {
        super(args);
        assureMatchVersion();

        String dictPath = args.get(DICT_PATH_ATTR);
        if (dictPath == null) {
            dictPath = DEFAULT_DICT_PATH;
        }

        String includeTagsStr = args.get(INCLUDE_TAGS_ATTR);
        List<Set<String>> includeTags = null;
        if (includeTags != null) {
            includeTags = new ArrayList<Set<String>>();
            for (String tagStr : includeTagsStr.split(" ")) {
                Set<String> grammemeValues = new HashSet<String>();
                includeTags.add(grammemeValues);
                for (String grammemeStr : tagStr.split(",")) {
                    grammemeValues.add(grammemeStr);
                }
            }
        }

        this.dictPath = dictPath;
        this.replacesPath = args.get(REPLACES_PATH_ATTR);
        this.includeTags = includeTags;
        this.includeUnknown = getBoolean(args, INCLUDE_UNKNOWN_ATTR, true);
    }

    public void inform(ResourceLoader loader) throws IOException {
        Map<Character,String> replaceChars = null;
        if (replacesPath != null) {
            replaceChars = parseReplaces(loader.openResource(replacesPath));
        }

        morph = new MorphAnalyzer(new SolrFileLoader(loader, dictPath), replaceChars);
    }

    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2StemFilter(tokenStream, morph, includeTags, includeUnknown);
    }

    @SuppressWarnings("unchecked")
    private Map<Character,String> parseReplaces(InputStream stream) throws IOException {
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
