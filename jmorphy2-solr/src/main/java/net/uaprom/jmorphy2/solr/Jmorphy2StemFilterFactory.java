package net.uaprom.jmorphy2.solr;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import net.uaprom.jmorphy2.JSONUtils;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class Jmorphy2StemFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    public static final String DICT_PATH_ATTR = "dict";
    public static final String REPLACES_PATH_ATTR = "replaces";
    public static final String IGNORE_NUMBERS_ATTR = "ignoreNumbers";
    
    public static final String DEFAULT_DICT_PATH = "dict";

    private MorphAnalyzer morph;
    private final String dictPath;
    private final String replacesPath;
    private final boolean ignoreNumbers; // if true don't try to stem numbers

    public Jmorphy2StemFilterFactory(Map<String,String> args) {
        super(args);
        assureMatchVersion();

        String dictPath = args.get(DICT_PATH_ATTR);
        if (dictPath == null) {
            dictPath = DEFAULT_DICT_PATH;
        }

        this.dictPath = dictPath;
        this.replacesPath = args.get(REPLACES_PATH_ATTR);
        this.ignoreNumbers = getBoolean(args, IGNORE_NUMBERS_ATTR, true);
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
        return new Jmorphy2StemFilter(tokenStream, morph, ignoreNumbers);
    }

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
