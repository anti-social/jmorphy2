package net.uaprom.jmorphy2;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import net.uaprom.jmorphy2.ResourceFileLoader;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class Jmorphy2TestsHelpers {
    public static MorphAnalyzer newMorphAnalyzer(String dictPath) throws IOException {
        return newMorphAnalyzer(dictPath, null);
    }

    public static MorphAnalyzer newMorphAnalyzer(String dictPath, Map<Character,String> replaceChars) throws IOException {
        return new MorphAnalyzer.Builder()
            .fileLoader(new ResourceFileLoader(dictPath))
            .charSubstitutes(replaceChars)
            .cacheSize(0)
            .build();
    }
}
