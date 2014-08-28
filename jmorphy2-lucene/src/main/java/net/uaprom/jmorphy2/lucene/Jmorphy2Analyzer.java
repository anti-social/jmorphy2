package net.uaprom.jmorphy2.lucene;

import java.io.Reader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ru.RussianLetterTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

import net.uaprom.jmorphy2.MorphAnalyzer;


public final class Jmorphy2Analyzer extends Analyzer {
    private static Map<Character, String> DEFAULT_REPLACES = new HashMap<Character, String>() {
        { put('е', "ё"); }
    };

    private static List<Set<String>> DEFAULT_EXCLUDE_TAGS = new ArrayList<Set<String>>();
    static {
        for (final String pos : new String[]{"NPRO", "PREP", "CONJ", "PRCL", "INTJ"}) {
            DEFAULT_EXCLUDE_TAGS.add(new HashSet() {
                    { add(pos); }
                });
        }
    }

    private final Version matchVersion;
    private final MorphAnalyzer morph;

    public Jmorphy2Analyzer(Version matchVersion, MorphAnalyzer morph) {
        super();
        this.matchVersion = matchVersion;
        this.morph = morph;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        final Tokenizer source = new StandardTokenizer(matchVersion, reader);
        TokenStream result = new LowerCaseFilter(matchVersion, source);
        result = new Jmorphy2StemFilter(result, morph, DEFAULT_EXCLUDE_TAGS, null, true);
        return new TokenStreamComponents(source, result);
    }
}