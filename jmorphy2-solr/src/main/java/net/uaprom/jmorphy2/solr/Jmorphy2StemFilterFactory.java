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
    public static final String TAG_LIST_ATTR = "tagList";
    
    public static final String DEFAULT_DICT_PATH = "pymorphy2_dicts";

    private MorphAnalyzer morph;
    private final String dictPath;
    private final String replacesPath;
    private final List<Set<String>> tagList;

    public Jmorphy2StemFilterFactory(Map<String,String> args) {
        super(args);
        assureMatchVersion();

        String dictPath = args.get(DICT_PATH_ATTR);
        if (dictPath == null) {
            dictPath = DEFAULT_DICT_PATH;
        }

        String tagListStr = args.get(TAG_LIST_ATTR);
        List<Set<String>> tagList = null;
        if (tagListStr != null) {
            tagList = new ArrayList<Set<String>>();
            for (String tagStr : tagListStr.split(" ")) {
                Set<String> grammemeValues = new HashSet<String>();
                tagList.add(grammemeValues);
                for (String grammemeStr : tagStr.split(",")) {
                    grammemeValues.add(grammemeStr);
                }
            }
        }

        this.dictPath = dictPath;
        this.replacesPath = args.get(REPLACES_PATH_ATTR);
        this.tagList = tagList;
    }

    public void inform(final ResourceLoader loader) throws IOException {
        Map<Character,String> replaceChars = null;
        if (replacesPath != null) {
            replaceChars = parseReplaces(loader.openResource(replacesPath));
        }

        morph = new MorphAnalyzer(new MorphAnalyzer.FileLoader() {
                @Override
                public InputStream getStream(String filename) throws IOException {
                    return loader.openResource((new File(dictPath, filename)).getPath());
                }
            },
            replaceChars);
    }

    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2StemFilter(tokenStream, morph, tagList);
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
