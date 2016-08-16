package net.uaprom.jmorphy2.lucene;

import java.io.IOException;

import net.uaprom.jmorphy2.MorphAnalyzer;
import net.uaprom.jmorphy2.Jmorphy2TestsHelpers;


public class BaseFilterTestCase {
    protected MorphAnalyzer morph;
    protected boolean initialized = false;

    protected void init() throws IOException {
        if (!initialized) {
            morph = Jmorphy2TestsHelpers.newMorphAnalyzer("/pymorphy2_dicts_ru");
            initialized = true;
        }
    }
}
