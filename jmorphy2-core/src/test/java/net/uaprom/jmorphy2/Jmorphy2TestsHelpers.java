package net.uaprom.jmorphy2;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import net.uaprom.jmorphy2.FileLoader;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class Jmorphy2TestsHelpers {
    private static final String DICT_PATH = "/pymorphy2_dicts";

    public static MorphAnalyzer newMorphAnalyzer() throws IOException {
        Map<Character,String> replaceChars = new HashMap<Character,String>() {
            { put('е', "ё"); }
        };
        return newMorphAnalyzer(replaceChars);
    }

    public static MorphAnalyzer newMorphAnalyzer(Map<Character,String> replaceChars) throws IOException {
        return new MorphAnalyzer(new FileLoader() {
                @Override
                public InputStream getStream(String filename) throws IOException {
                    return getClass().getResourceAsStream(DICT_PATH + "/" + filename);
                }
            },
            replaceChars,
            0);
    }
}
