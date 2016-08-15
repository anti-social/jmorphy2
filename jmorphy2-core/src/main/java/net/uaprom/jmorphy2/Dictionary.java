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
    private String[] paradigmPrefixes;
    private String[] suffixes;
    private Tag[] gramtab;

    public static final String META_FILENAME = "meta.json";
    public static final String WORDS_FILENAME = "words.dawg";
    public static final String GRAMMEMES_FILENAME = "grammemes.json";
    public static final String PARADIGMS_FILENAME = "paradigms.array";
    public static final String SUFFIXES_FILENAME = "suffixes.json";
    public static final String GRAMTAB_OPENCORPORA_FILENAME = "gramtab-opencorpora-int.json";

    private Dictionary(Tag.Storage tagStorage, FileLoader loader) throws IOException {
        this(tagStorage,
             loader.getStream(META_FILENAME),
             loader.getStream(WORDS_FILENAME),
             loader.getStream(GRAMMEMES_FILENAME),
             loader.getStream(PARADIGMS_FILENAME),
             loader.getStream(SUFFIXES_FILENAME),
             loader.getStream(GRAMTAB_OPENCORPORA_FILENAME));
    }

    private Dictionary(Tag.Storage tagStorage,
                       InputStream metaStream,
                       InputStream wordsStream,
                       InputStream grammemesStream,
                       InputStream paradigmsStream,
                       InputStream suffixesStream,
                       InputStream gramtabStream) throws IOException {
        this.tagStorage = tagStorage;
        loadMeta(metaStream);
        words = new WordsDAWG(wordsStream);
        loadGrammemes(grammemesStream);
        loadParadigms(paradigmsStream);
        loadSuffixes(suffixesStream);
        loadGramtab(gramtabStream);
    }

    public static class Builder {
        private FileLoader loader;

        public Builder(FileLoader loader) {
            this.loader = loader;
        }

        public Dictionary build(Tag.Storage tagStorage) throws IOException {
            return new Dictionary(tagStorage, loader);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadMeta(InputStream stream) throws IOException {
        meta = new HashMap<String,Object>();
        List<List<Object>> parsed = (List<List<Object>>) JSONUtils.parseJSON(stream);
        for (List<Object> pair : parsed) {
            meta.put((String) pair.get(0), pair.get(1));
        }
        Map<String,Object> compileOptions = (Map<String,Object>) meta.get("compile_options");
        paradigmPrefixes = ((List<String>) compileOptions.get("paradigm_prefixes")).toArray(new String[0]);
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
    private void loadGramtab(InputStream stream) throws IOException {
        List<String> tagStrings = (List<String>) JSONUtils.parseJSON(stream);
        int tagsLength = tagStrings.size();
        gramtab = new Tag[tagsLength];
        for (int i = 0; i < tagsLength; i++) {
            gramtab[i] = tagStorage.newTag(tagStrings.get(i));
        }
    }

    // public List<Parsed> parse(char[] word, int offset, int count) throws IOException {
    //     return parse(new String(word, offset, count));
    // }

    // public List<Parsed> parse(String word) throws IOException {
    //     List<Parsed> parseds = new ArrayList<Parsed>();

    //     for (WordsDAWG.WordForm wordForm : words.similarWords(word, replaceChars)) {
    //         Paradigm paradigm = paradigms[wordForm.paradigmId];
    //         String normalForm = buildNormalForm(paradigm, wordForm.idx, wordForm.word);
    //         Tag tag = buildTag(paradigm, wordForm.idx);
    //         parseds.add(new Parsed(word, tag, normalForm, wordForm));
    //     }
        
    //     return parseds;
    // }

    // TODO: benchmark iterators
    public List<Parsed> parse(String word, Map<Character, String> replaceChars)
        throws IOException
    {
        List<Parsed> parseds = new ArrayList<>();

        for (WordsDAWG.WordForm wordForm : words.similarWords(word, replaceChars)) {
            String normalForm = buildNormalForm(wordForm.paradigmId, wordForm.idx, wordForm.word);
            Tag tag = buildTag(wordForm.paradigmId, wordForm.idx);
            parseds.add(new Parsed(word, tag, normalForm, wordForm));
        }
        
        return parseds;
    }

    public Paradigm getParadigm(short paradigmId) {
        return paradigms[paradigmId];
    }
    
    public String getSuffix(short paradigmId, short idx) {
        return suffixes[paradigms[paradigmId].getStemSuffixId(idx)];
    }
    
    public Tag buildTag(short paradigmId, short idx) {
        Paradigm paradigm = paradigms[paradigmId];
        return gramtab[paradigm.getTagId(idx)];
    }

    public String buildNormalForm(short paradigmId, short idx, String word) {
        Paradigm paradigm = paradigms[paradigmId];
        String stem = buildStem(paradigmId, idx, word);
        String prefix = paradigmPrefixes[paradigm.getNormPrefixId()];
        String suffix = suffixes[paradigm.getNormSuffixId()];
        
        return prefix + stem + suffix;
    }

    public String buildStem(short paradigmId, short idx, String word) {
        Paradigm paradigm = paradigms[paradigmId];
        String prefix = paradigmPrefixes[paradigm.getStemPrefixId(idx)];
        String suffix = suffixes[paradigm.getStemSuffixId(idx)];

        if (!suffix.equals("")) {
            return word.substring(prefix.length(), word.length() - suffix.length());
        }
        return word.substring(prefix.length());
    }

    public class Parsed {
        public final String word;
        public final Tag tag;
        public final String normalForm;
        public final WordsDAWG.WordForm wordForm;

        public Parsed(String word, Tag tag, String normalForm, WordsDAWG.WordForm wordForm) {
            this.word = word;
            this.tag = tag;
            this.normalForm = normalForm;
            this.wordForm = wordForm;
        }

        public List<Parsed> iterLexeme() {
            List<Parsed> lexeme = new ArrayList<>();
            Paradigm paradigm = Dictionary.this.getParadigm(wordForm.paradigmId);
            int paradigmSize = paradigm.size();
            String stem = Dictionary.this.buildStem(wordForm.paradigmId, wordForm.idx, wordForm.word);
            String normalForm = Dictionary.this.buildNormalForm(wordForm.paradigmId, wordForm.idx, wordForm.word);
            for (short idx = 0; idx < paradigmSize; idx++) {
                String prefix = paradigmPrefixes[paradigm.getStemPrefixId(idx)];
                String suffix = Dictionary.this.getSuffix(wordForm.paradigmId, idx);
                String word = prefix + stem + suffix;
                Tag tag = Dictionary.this.buildTag(wordForm.paradigmId, idx);
                WordsDAWG.WordForm wf = new WordsDAWG.WordForm(word, wordForm.paradigmId, idx);
                lexeme.add(new Parsed(word, tag, normalForm, wf));
            }
            return lexeme;
        }
    };

    static public class Paradigm {
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

        public int size() {
            return length;
        }
    };

    public class ParadigmInfo {
        public final String prefix;
        public final String suffix;
        public final Tag tag;

        public ParadigmInfo(String prefix, String suffix, Tag tag) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.tag = tag;
        }
    };
}
