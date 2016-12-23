package company.evo.jmorphy2;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import company.evo.jmorphy2.ResourceFileLoader;
import company.evo.jmorphy2.MorphAnalyzer;


public class Jmorphy2TestsHelpers {
    public static MorphAnalyzer newMorphAnalyzer(String lang) throws IOException {
        return newMorphAnalyzer(lang, null);
    }

    public static MorphAnalyzer newMorphAnalyzer(String lang, Map<Character,String> replaceChars)
        throws IOException
    {
        String dictResourcePath = String.format("/company/evo/jmorphy2/pymorphy2_dicts_%s", lang);
        return new MorphAnalyzer.Builder()
            .fileLoader(new ResourceFileLoader(dictResourcePath))
            .charSubstitutes(replaceChars)
            .cacheSize(0)
            .build();
    }
}
