package net.uaprom.jmorphy2;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;


public class BaseTestCase {
    protected MorphAnalyzer analyzer;

    public BaseTestCase() throws IOException {
        final String dictPath = "/dict";
        Map<Character,String> replaceChars = new HashMap<Character,String>();
        replaceChars.put('е', "ё");
        analyzer = new MorphAnalyzer(new MorphAnalyzer.FileLoader() {
                @Override
                public InputStream getStream(String filename) throws IOException {
                    return getClass().getResourceAsStream((new File(dictPath, filename)).getPath());
                }
            },
            replaceChars,
            0);
    }
}
