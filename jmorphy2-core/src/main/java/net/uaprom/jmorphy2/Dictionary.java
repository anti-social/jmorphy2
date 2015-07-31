package net.uaprom.jmorphy2;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Collection;

import org.apache.commons.io.input.SwappedDataInputStream;

import net.uaprom.dawg.PayloadsDAWG;


public class Dictionary {
    private final Tag.Storage tagStorage;
    private Map<String,Object> meta;
    private WordsDAWG words;
    private Paradigm[] paradigms;
    private String[] suffixes;
    private String[] paradigmPrefixes;
    private Tag[] gramtab;
    private Map<Character,String> replaceChars;

    public static final String META_FILENAME = "meta.json";
    public static final String WORDS_FILENAME = "words.dawg";
    public static final String GRAMMEMES_FILENAME = "grammemes.json";
    public static final String PARADIGMS_FILENAME = "paradigms.array";
    public static final String SUFFIXES_FILENAME = "suffixes.json";
    public static final String PARADIGM_PREFIXES_FILENAME = "paradigm-prefixes.json";
    public static final String GRAMTAB_OPENCORPORA_FILENAME = "gramtab-opencorpora-int.json";

    public Dictionary(Tag.Storage tagStorage, FileLoader loader, Map<Character,String> replaceChars) throws IOException {
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

    @SuppressWarnings("unchecked")
    private void loadMeta(InputStream stream) throws IOException {
        meta = new HashMap<String,Object>();
        List<List<Object>> parsed = (List<List<Object>>) JSONUtils.parseJSON(stream);
        for (List<Object> pair : parsed) {
            meta.put((String) pair.get(0), pair.get(1));
        }
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    private void loadSuffixes(InputStream stream) throws IOException {
        suffixes = ((List<String>) JSONUtils.parseJSON(stream)).toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    private void loadParadigmPrefixes(InputStream stream) throws IOException {
        paradigmPrefixes = ((List<String>) JSONUtils.parseJSON(stream)).toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    private void loadGramtab(InputStream stream) throws IOException {
        List<String> tagStrings = (List<String>) JSONUtils.parseJSON(stream);
        int tagsLength = tagStrings.size();
        gramtab = new Tag[tagsLength];
        for (int i = 0; i < tagsLength; i++) {
            gramtab[i] = tagStorage.newTag(tagStrings.get(i));
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
            WordsDAWG.FoundParadigm fp = (WordsDAWG.FoundParadigm) item;
            Paradigm paradigm = paradigms[fp.paraId];
            String nf = buildNormalForm(paradigm, fp.idx, fp.key);
            Tag tag = buildTag(paradigm, fp.idx);
            parseds.add(new Parsed(word, tag, nf, word, fp));
        }
        
        return parseds;
    }

    protected Tag buildTag(Paradigm paradigm, short idx) {
        return gramtab[paradigm.getTagId(idx)];
    }

    protected String buildNormalForm(Paradigm paradigm, short idx, String word) {
        String stem = buildStem(paradigm, idx, word);
        String prefix = paradigmPrefixes[paradigm.getNormPrefixId()];
        String suffix = suffixes[paradigm.getNormSuffixId()];
        
        return prefix + stem + suffix;
    }

    protected String buildStem(Paradigm paradigm, short idx, String word) {
        String prefix = paradigmPrefixes[paradigm.getStemPrefixId(idx)];
        String suffix = suffixes[paradigm.getStemSuffixId(idx)];

        if (!suffix.equals("")) {
            return word.substring(prefix.length(), word.length() - suffix.length());
        }
        return word.substring(prefix.length());
    }

    class Parsed {
        public final String word;
        public final Tag tag;
        public final String normalForm;
        public final String foundWord;
        public final WordsDAWG.FoundParadigm foundParadigm;

        public Parsed(String word, Tag tag, String normalForm, String foundWord, WordsDAWG.FoundParadigm foundParadigm) {
            this.word = word;
            this.tag = tag;
            this.normalForm = normalForm;
            this.foundWord = foundWord;
            this.foundParadigm = foundParadigm;
        }
    };

    class Paradigm {
        private final short[] data;
        private final int length;

        public Paradigm(DataInput input) throws IOException {
            short size = input.readShort();
            this.data = new short[size];
            for (int i = 0; i < size; i++) {
                this.data[i] = input.readShort();
            }
            this.length = size / 3;
        }

        public int getNormSuffixId() {
            return data[0];
        }

        public int getNormPrefixId() {
            return data[length * 2];
        }

        public int getStemSuffixId(short idx) {
            return data[idx];
        }

        public int getStemPrefixId(short idx) {
            return data[length * 2 + idx];
        }

        public int getTagId(short idx) {
            return data[length + idx];
        }
    };
}
