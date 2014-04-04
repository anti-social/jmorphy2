package net.uaprom.jmorphy2;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.input.SwappedDataInputStream;

import net.uaprom.dawg.PayloadsDAWG;


public class Dictionary {
    private final Tag.Storage tagStorage;
    private Map<String,Object> meta;
    private WordsDAWG words;
    private Paradigm[] paradigms;
    private String[] suffixes;
    private String[] paradigmPrefixes;
    private List<Tag> gramtab;
    private Map<Character,String> replaceChars;

    public static final String META_FILENAME = "meta.json";
    public static final String WORDS_FILENAME = "words.dawg";
    public static final String GRAMMEMES_FILENAME = "grammemes.json";
    public static final String PARADIGMS_FILENAME = "paradigms.array";
    public static final String SUFFIXES_FILENAME = "suffixes.json";
    public static final String PARADIGM_PREFIXES_FILENAME = "paradigm-prefixes.json";
    public static final String GRAMTAB_OPENCORPORA_FILENAME = "gramtab-opencorpora-int.json";

    private static final Logger logger = LoggerFactory.getLogger(Dictionary.class);

    public Dictionary(Tag.Storage tagStorage, MorphAnalyzer.FileLoader loader, Map<Character,String> replaceChars) throws IOException {
        this(tagStorage,
             loader.getStream(META_FILENAME),
             loader.getStream(WORDS_FILENAME),
             loader.getStream(GRAMMEMES_FILENAME),
             loader.getStream(PARADIGMS_FILENAME),
             loader.getStream(SUFFIXES_FILENAME),
             loader.getStream(PARADIGM_PREFIXES_FILENAME),
             loader.getStream(GRAMTAB_OPENCORPORA_FILENAME),
             replaceChars);
    }

    protected Dictionary(Tag.Storage tagStorage,
                         InputStream metaStream,
                         InputStream wordsStream,
                         InputStream grammemesStream,
                         InputStream paradigmsStream,
                         InputStream suffixesStream,
                         InputStream paradigmPrefixesStream,
                         InputStream gramtabStream,
                         Map<Character,String> replaceChars) throws IOException {
        this.tagStorage = tagStorage;
        loadMeta(metaStream);
        words = new WordsDAWG(wordsStream);
        loadGrammemes(grammemesStream);
        loadParadigms(paradigmsStream);
        loadSuffixes(suffixesStream);
        loadParadigmPrefixes(paradigmPrefixesStream);
        loadGramtab(gramtabStream);
        this.replaceChars = replaceChars;
    }

    private void loadMeta(InputStream stream) throws IOException {
        meta = new HashMap<String,Object>();
        List<List<Object>> parsed = (List<List<Object>>) JSONUtils.parseJSON(stream);
        for (List<Object> pair : parsed) {
            meta.put((String) pair.get(0), pair.get(1));
        }
    }

    private void loadGrammemes(InputStream stream) throws IOException {
        for (List<String> grammemeInfo : (List<List<String>>) JSONUtils.parseJSON(stream)) {
            tagStorage.newGrammeme(grammemeInfo);
        }
    }

    private void loadParadigms(InputStream stream) throws IOException {
        DataInput paradigmsStream = new SwappedDataInputStream(stream);
        short paradigmCount = paradigmsStream.readShort();
        paradigms = new Paradigm[paradigmCount];
        for (int paraId = 0; paraId < paradigmCount; paraId++) {
            paradigms[paraId] = new Paradigm(paradigmsStream);
        }
    }

    private void loadSuffixes(InputStream stream) throws IOException {
        suffixes = ((List<String>) JSONUtils.parseJSON(stream)).toArray(new String[0]);
    }

    private void loadParadigmPrefixes(InputStream stream) throws IOException {
        paradigmPrefixes = ((List<String>) JSONUtils.parseJSON(stream)).toArray(new String[0]);
    }

    private void loadGramtab(InputStream stream) throws IOException {
        gramtab = new ArrayList<Tag>();
        for (String tagString : (List<String>) JSONUtils.parseJSON(stream)) {
            gramtab.add(tagStorage.newTag(tagString));
        }
    }

    public List<Parsed> parse(char[] word, int offset, int count) throws IOException {
        return parse(new String(word, offset, count));
    }

    public List<Parsed> parse(String word) throws IOException {
        List<String> normalForms = new ArrayList<String>();
        List<PayloadsDAWG.Payload> items = words.similarItems(word, replaceChars);;
        List<Parsed> parseds = new ArrayList<Parsed>();

        for (PayloadsDAWG.Payload item : items) {
            WordsDAWG.FoundParadigm paradigm = (WordsDAWG.FoundParadigm) item;
            String nf = buildNormalForm(paradigm.paraId,
                                        paradigm.idx,
                                        paradigm.key);
            Tag tag = buildTag(paradigm.paraId, paradigm.idx);
            parseds.add(new Parsed(word, tag, nf, word, 1.0f));
        }
        
        return parseds;
    }

    protected Tag buildTag(short paraId, short idx) {
        Paradigm paradigm = paradigms[paraId];
        int offset = paradigm.paradigm.length / 3;
        int tagId = paradigm.paradigm[offset + idx];
        return gramtab.get(tagId);
    }

    protected String buildNormalForm(short paraId, short idx, String word) {
        Paradigm paradigm = paradigms[paraId];
        int paradigmLength = paradigm.paradigm.length / 3;
        String stem = buildStem(paradigm.paradigm, idx, word);

        int prefixId = paradigm.paradigm[paradigmLength * 2 + 0] & 0xFFFF;
        int suffixId = paradigm.paradigm[0] & 0xFFFF;

        String prefix = paradigmPrefixes[prefixId];
        String suffix = suffixes[suffixId];
        
        return prefix + stem + suffix;
    }

    protected String buildStem(short[] paradigm, short idx, String word) {
        int paradigmLength = paradigm.length / 3;
        int prefixId = paradigm[paradigmLength * 2 + idx] & 0xFFFF;
        String prefix = paradigmPrefixes[prefixId];
        int suffixId = paradigm[idx] & 0xFFFF;
        String suffix = suffixes[suffixId];

        if (!suffix.equals("")) {
            return word.substring(prefix.length(), word.length() - suffix.length());
        }
        else {
            return word.substring(prefix.length());
        }
    }

    public class WordsDAWG extends PayloadsDAWG {
        public WordsDAWG(InputStream stream) throws IOException {
            super(stream);
        }

        @Override
        protected Payload newPayload(String key, byte[] value) throws IOException {
            return new FoundParadigm(key, value);
        }

        public class FoundParadigm extends PayloadsDAWG.Payload {
            public final short paraId;
            public final short idx;

            public FoundParadigm(String key, byte[] value) throws IOException {
                super(key, value);

                DataInput stream = new DataInputStream(new ByteArrayInputStream(this.value));
                this.paraId = stream.readShort();
                this.idx = stream.readShort();
            }
        };
    };

    public static class Paradigm {
        public short[] paradigm;

        public Paradigm(DataInput input) throws IOException {
            short length = input.readShort();
            paradigm = new short[length];
            for (int i = 0; i < length; i++) {
                paradigm[i] = input.readShort();
            }
        }

        public short[] getParadigm() {
            return paradigm;
        }
    };
}
