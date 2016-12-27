package company.evo.jmorphy2.lucene;

import java.io.IOException;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.Jmorphy2TestsHelpers;


public class BaseFilterTestCase {
    protected MorphAnalyzer morph = null;

    protected void init() throws IOException {
        if (morph == null) {
            morph = Jmorphy2TestsHelpers.newMorphAnalyzer("ru");
        }
    }
}
