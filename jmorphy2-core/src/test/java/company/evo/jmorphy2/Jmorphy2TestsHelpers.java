package company.evo.jmorphy2;

import java.io.IOException;
import java.util.Map;

public class Jmorphy2TestsHelpers {
    public static MorphAnalyzer newMorphAnalyzer(String lang) throws IOException {
        return newMorphAnalyzer(lang, null);
    }

    public static MorphAnalyzer newMorphAnalyzer(String lang, Map<Character,String> replaceChars)
        throws IOException
    {
        String dictResourcePath = String.format("/company/evo/jmorphy2/%s/pymorphy2_dicts", lang);
        return new MorphAnalyzer.Builder<>()
            .fileLoader(new ResourceFileLoader(dictResourcePath))
            .charSubstitutes(replaceChars)
            .build();
    }
}
