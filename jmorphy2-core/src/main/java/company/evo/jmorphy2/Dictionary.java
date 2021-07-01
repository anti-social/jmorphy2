package company.evo.jmorphy2;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.input.SwappedDataInputStream;

public final class Dictionary {
    private final Meta meta;
    private final String[] paradigmPrefixes;
    private final WordsDAWG words;
    private final SuffixesDAWG[] predictionSuffixes;
    private final Paradigm[] paradigms;
    private final String[] suffixes;
    private final Tag[] gramtab;

    private Dictionary(Meta meta,
                       WordsDAWG words,
                       SuffixesDAWG[] predictionSuffixes,
                       Paradigm[] paradigms,
                       String[] suffixes,
                       Tag[] gramtab) {
        this.meta = meta;
        this.paradigmPrefixes = meta.compileOptions.paradigmPrefixes;
        this.words = words;
        this.predictionSuffixes = predictionSuffixes;
        this.paradigms = paradigms;
        this.suffixes = suffixes;
        this.gramtab = gramtab;
    }

    public static class Builder {
        private final FileLoader loader;
        private Dictionary cachedDict;

        public static final String META_FILENAME = "meta.json";
        public static final String WORDS_FILENAME = "words.dawg";
        public static final String PARADIGMS_FILENAME = "paradigms.array";
        public static final String SUFFIXES_FILENAME = "suffixes.json";
        public static final String PREDICTION_SUFFIXES_FILENAME_TEMPLATE = "prediction-suffixes-%s.dawg";
        public static final String GRAMMEMES_FILENAME = "grammemes.json";
        public static final String GRAMTAB_OPENCORPORA_FILENAME = "gramtab-opencorpora-int.json";

        public Builder(FileLoader loader) {
            this.loader = loader;
        }

        @SuppressWarnings("unchecked")
        private Meta parseMeta(InputStream stream) throws IOException {
            Map<String,Object> rawMeta = new HashMap<>();
            List<List<Object>> parsed = (List<List<Object>>) JSONUtils.parseJSON(stream);
            for (List<Object> pair : parsed) {
                rawMeta.put((String) pair.get(0), pair.get(1));
            }
            return new Meta(rawMeta);
        }

        private SuffixesDAWG[] parsePredictionSuffixes(FileLoader loader,
                                                       String filenameTemplate,
                                                       int num)
            throws IOException
        {
            SuffixesDAWG[] predictionSuffixes = new SuffixesDAWG[num];
            for (int i = 0; i < num; i++) {
                InputStream suffixesStream = loader.newStream(String.format(filenameTemplate, i));
                predictionSuffixes[i] = new SuffixesDAWG(suffixesStream);
                suffixesStream.close();
            }
            return predictionSuffixes;
        }

        private Paradigm[] parseParadigms(InputStream stream) throws IOException {
            DataInput paradigmsStream = new SwappedDataInputStream(stream);
            short paradigmCount = paradigmsStream.readShort();
            Paradigm[] paradigms = new Paradigm[paradigmCount];
            for (int paraId = 0; paraId < paradigmCount; paraId++) {
                paradigms[paraId] = new Paradigm(paradigmsStream);
            }
            return paradigms;
        }

        @SuppressWarnings("unchecked")
        private String[] parseSuffixes(InputStream stream) throws IOException {
            return ((List<String>) JSONUtils.parseJSON(stream)).toArray(new String[0]);
        }

        @SuppressWarnings("unchecked")
        private void loadGrammemes(Tag.Storage tagStorage, InputStream stream) throws IOException {
            for (List<String> grammemeInfo : (List<List<String>>) JSONUtils.parseJSON(stream)) {
                tagStorage.newGrammeme(grammemeInfo);
            }
        }

        @SuppressWarnings("unchecked")
        private Tag[] parseGramtab(Tag.Storage tagStorage, InputStream stream) throws IOException {
            List<String> tagStrings = (List<String>) JSONUtils.parseJSON(stream);
            int tagsLength = tagStrings.size();
            Tag[] gramtab = new Tag[tagsLength];
            for (int i = 0; i < tagsLength; i++) {
                gramtab[i] = tagStorage.newTag(tagStrings.get(i));
            }
            return gramtab;
        }

        public Dictionary build(Tag.Storage tagStorage) throws IOException {
            if (cachedDict == null) {
                InputStream metaStream = loader.newStream(META_FILENAME);
                Meta meta = parseMeta(metaStream);
                metaStream.close();
                InputStream grammemesStream = loader.newStream(GRAMMEMES_FILENAME);
                loadGrammemes(tagStorage, grammemesStream);
                grammemesStream.close();
                InputStream wordsStream = loader.newStream(WORDS_FILENAME);
                InputStream paradigmsStream = loader.newStream(PARADIGMS_FILENAME);
                InputStream suffixesStream = loader.newStream(SUFFIXES_FILENAME);
                InputStream gramtabStream = loader.newStream(GRAMTAB_OPENCORPORA_FILENAME);
                cachedDict = new Dictionary(
                    meta,
                    new WordsDAWG(wordsStream),
                    parsePredictionSuffixes(loader,
                        PREDICTION_SUFFIXES_FILENAME_TEMPLATE,
                        meta.compileOptions.paradigmPrefixes.length
                    ),
                    parseParadigms(paradigmsStream),
                    parseSuffixes(suffixesStream),
                    parseGramtab(tagStorage, gramtabStream)
                );
                wordsStream.close();
                paradigmsStream.close();
                suffixesStream.close();
                gramtabStream.close();
            }
            return cachedDict;
        }
    }

    public Meta getMeta() {
        return meta;
    }

    public WordsDAWG getWords() {
        return words;
    }

    public SuffixesDAWG getPredictionSuffixes(int n) {
        return predictionSuffixes[n];
    }

    public String[] getParadigmPrefixes() {
        return paradigmPrefixes;
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
            ptw = (boolean) (meta.getOrDefault("P(t|w)", false));
            ptwUniqueWords = (long) (meta.getOrDefault("P(t|w)_unique_words", -1L));
            ptwOutcomes = (long) (meta.getOrDefault("P(t|w)_outcomes", -1L));
            ptwMinWordFreq = (long) (meta.getOrDefault("P(t|w)_min_word_freq", -1L));
            corpusRevision = (String) (meta.getOrDefault("corpus_revision", ""));
        }
    }

    static public class Paradigm {
        private final short[] data;
        private final int length;

        public Paradigm(DataInput input) throws IOException {
            short size = input.readShort();
            assert size % 3 == 0 : size;

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
    }
}
