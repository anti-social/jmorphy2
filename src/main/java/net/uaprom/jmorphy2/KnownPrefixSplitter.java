package net.uaprom.jmorphy2;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import net.uaprom.dawg.DAWG;


public class KnownPrefixSplitter {
    public static final String PREDICTION_PREFIXES_FILENAME = "prediction-prefixes.dawg";

    private DAWG dict;

    public KnownPrefixSplitter(MorphAnalyzer.Loader loader) throws IOException {
        this(loader.getStream(PREDICTION_PREFIXES_FILENAME));
    }
    
    public KnownPrefixSplitter(InputStream stream) throws IOException {
        dict = new DAWG(stream);
    }

    public List<String> prefixes(String word) throws IOException {
        return dict.prefixes(word);
    }

    public List<String> prefixes(String word, int minReminder) throws IOException {
        List<String> res = new ArrayList<String>();
        for (String prefix : dict.prefixes(word)) {
            if (word.length() - prefix.length() > minReminder) {
                res.add(prefix);
            }
        }
        return res;
    }
}
