package net.uaprom.jmorphy2.lucene;

import java.io.IOException;

import org.apache.lucene.util.Version;

import net.uaprom.jmorphy2.MorphAnalyzer;
import net.uaprom.jmorphy2.Jmorphy2TestsHelpers;


public class BaseFilterTestCase {
    protected static final Version LUCENE_VERSION = Version.LUCENE_4_9;

    protected MorphAnalyzer morph;
    protected boolean initialized = false;

    protected void init() throws IOException {
        if (!initialized) {
            morph = Jmorphy2TestsHelpers.newMorphAnalyzer();
            initialized = true;
        }
    }
}
