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
    private Meta meta;
    private WordsDAWG words;
    public PredictionSuffixesDAWG predictionSuffixes;
    private Paradigm[] paradigms;
    private String[] paradigmPrefixes;
    private String[] suffixes;
    private Tag[] gramtab;

    public static final String META_FILENAME = "meta.json";
    public static final String WORDS_FILENAME = "words.dawg";
    public static final String GRAMMEMES_FILENAME = "grammemes.json";
    public static final String PARADIGMS_FILENAME = "paradigms.array";
    public static final String SUFFIXES_FILENAME = "suffixes.json";
    public static final String PREDICTION_SUFFIXES_FILENAME = "prediction-suffixes-0.dawg";
    public static final String GRAMTAB_OPENCORPORA_FILENAME = "gramtab-opencorpora-int.json";

    private Dictionary(Tag.Storage tagStorage, FileLoader loader) throws IOException {
        this(tagStorage,
             loader.getStream(META_FILENAME),
             loader.getStream(WORDS_FILENAME),
             loader.getStream(PREDICTION_SUFFIXES_FILENAME),
             loader.getStream(GRAMMEMES_FILENAME),
             loader.getStream(PARADIGMS_FILENAME),
             loader.getStream(SUFFIXES_FILENAME),
             loader.getStream(GRAMTAB_OPENCORPORA_FILENAME));
    }

    private Dictionary(Tag.Storage tagStorage,
                       InputStream metaStream,
                       InputStream wordsStream,
                       InputStream predictionSuffixesStream,
                       InputStream grammemesStream,
                       InputStream paradigmsStream,
                       InputStream suffixesStream,
                       InputStream gramtabStream) throws IOException {
        this.tagStorage = tagStorage;
        loadMeta(metaStream);
        words = new WordsDAWG(wordsStream);
        loadPredictionSuffixes(predictionSuffixesStream);
        loadGrammemes(grammemesStream);
        loadParadigms(paradigmsStream);
        loadSuffixes(suffixesStream);
        loadGramtab(gramtabStream);
    }

    public static class Builder {
        private FileLoader loader;
        private Dictionary cachedDict;

        public Builder(FileLoader loader) {
            this.loader = loader;
        }

        public Dictionary build(Tag.Storage tagStorage) throws IOException {
            if (cachedDict == null) {
                cachedDict = new Dictionary(tagStorage, loader);
            }
            return cachedDict;
        }
    }

    @SuppressWarnings("unchecked")
    private void loadMeta(InputStream stream) throws IOException {
        Map<String,Object> rawMeta = new HashMap<>();
        List<List<Object>> parsed = (List<List<Object>>) JSONUtils.parseJSON(stream);
        for (List<Object> pair : parsed) {
            rawMeta.put((String) pair.get(0), pair.get(1));
        }
        meta = new Meta(rawMeta);
        paradigmPrefixes = meta.compileOptions.paradigmPrefixes;
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

    private void loadPredictionSuffixes(InputStream stream) throws IOException {
        predictionSuffixes = new PredictionSuffixesDAWG(stream);
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

    public Meta getMeta() {
        return meta;
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

    public static class Meta {
        public static final String FORMAT_VERSION = "2.4";

        public final String formatVersion;
        public final String pymorphy2Version;
        public final String languageCode;
        public final String compiledAt;
        public final String source;
        public final String sourceVersion;
        public final String sourceRevision;
        public final long sourceLexemesCount;
        public final long sourceLinksCount;
        public final long gramtabLength;
        public final Map<String,String> gramtabFormats;
        public final long paradigmsLength;
        public final long suffixesLength;
        public final long wordsDawgLength;
        public final CompileOptions compileOptions;
        public final Long[] predictionSuffixesDawgLengths;
        public final boolean ptw;
        public final long ptwUniqueWords;
        public final long ptwOutcomes;
        public final long ptwMinWordFreq;
        public final String corpusRevision;

        public static class CompileOptions {
            public final long maxSuffixLength;
            public final String[] paradigmPrefixes;
            public final long minEndingFreq;
            public final long minParadigmPopularity;

            @SuppressWarnings("unchecked")
            public CompileOptions(Map<String,Object> options) {
                maxSuffixLength = (long) options.get("max_suffix_length");
                paradigmPrefixes = ((List<String>) options.get("paradigm_prefixes"))
                    .toArray(new String[0]);
                minEndingFreq = (long) options.get("min_ending_freq");
                minParadigmPopularity = (long) options.get("min_paradigm_popularity");
            }
        }
        
        @SuppressWarnings("unchecked")
        public Meta(Map<String,Object> meta) {
            formatVersion = (String) meta.get("format_version");
            if (!formatVersion.equals(FORMAT_VERSION)) {
                throw new RuntimeException(String.format(
                    "Unsupported format version: %s, expected %s", formatVersion, FORMAT_VERSION));
            }
            pymorphy2Version = (String) meta.get("pymorphy2_version");
            languageCode = ((String) meta.get("language_code")).toLowerCase();
            compiledAt = (String) meta.get("compile_at");
            source = (String) meta.get("source");
            sourceVersion = (String) meta.get("source_version");
            sourceRevision = (String) meta.get("source_revision");
            sourceLexemesCount = (long) meta.get("source_lexemes_count");
            sourceLinksCount = (long) meta.get("source_links_count");
            gramtabLength = (long) meta.get("gramtab_length");
            gramtabFormats = (Map<String,String>) meta.get("gramtab_formats");
            paradigmsLength = (long) meta.get("paradigms_length");
            suffixesLength = (long) meta.get("suffixes_length");
            wordsDawgLength = (long) meta.get("words_dawg_length");
            compileOptions = new CompileOptions((Map<String,Object>) meta.get("compile_options"));
            predictionSuffixesDawgLengths = ((List<Long>) meta.get("prediction_suffixes_dawg_lengths"))
                .toArray(new Long[0]);
            ptw = (boolean) (meta.containsKey("P(t|w)") ? meta.get("P(t|w)") : false);
            ptwUniqueWords = (long) (meta.containsKey("P(t|w)_unique_words") ? meta.get("P(t|w)_unique_words") : -1L);
            ptwOutcomes = (long) (meta.containsKey("P(t|w)_outcomes") ? meta.get("P(t|w)_outcomes") : -1L);
            ptwMinWordFreq = (long) (meta.containsKey("P(t|w)_min_word_freq") ? meta.get("P(t|w)_min_word_freq") : -1L);
            corpusRevision = (String) (meta.containsKey("corpus_revision") ? meta.get("corpus_revision") : "");
        }
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
