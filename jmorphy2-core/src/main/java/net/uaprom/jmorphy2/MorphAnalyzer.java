package net.uaprom.jmorphy2;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.text.Normalizer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Floats;


public class MorphAnalyzer {
    private final Tag.Storage tagStorage;
    private final Dictionary dict;
    private final KnownPrefixSplitter knownPrefixSplitter;
    private final List<AnalyzerUnit> units;
    private final ProbabilityEstimator prob;
    private Cache<String,List<Parsed>> cache = null;

    // private static final String ENV_DICT_PATH = "PYMORPHY2_DICT_PATH";
    private static final String DICT_PATH_VAR = "dictPath";

    private static final int DEFAULT_CACHE_SIZE = 10000;

    public static abstract class FileLoader {
        public abstract InputStream getStream(String filename) throws IOException;
    }

    public static class FSFileLoader extends FileLoader {
        private String path;

        public FSFileLoader(String path) {
            this.path = path;
        }

        @Override
        public InputStream getStream(String filename) throws IOException {
            return new FileInputStream(new File(path, filename));
        }
    }

    public MorphAnalyzer() throws IOException {
        this(System.getProperty(DICT_PATH_VAR), null);
    }

    public MorphAnalyzer(String path) throws IOException {
        this(path, null);
    }

    public MorphAnalyzer(Map<Character,String> replaceChars) throws IOException {
        this(System.getProperty(DICT_PATH_VAR), replaceChars, DEFAULT_CACHE_SIZE);
    }

    public MorphAnalyzer(Map<Character,String> replaceChars, int cacheSize) throws IOException {
        this(System.getProperty(DICT_PATH_VAR), replaceChars, cacheSize);
    }

    public MorphAnalyzer(String path, Map<Character,String> replaceChars) throws IOException {
        this(new FSFileLoader(path), replaceChars, DEFAULT_CACHE_SIZE);
    }

    public MorphAnalyzer(String path, Map<Character,String> replaceChars, int cacheSize) throws IOException {
        this(new FSFileLoader(path), replaceChars, cacheSize);
    }

    public MorphAnalyzer(FileLoader loader, Map<Character,String> replaceChars) throws IOException {
        this(loader, replaceChars, DEFAULT_CACHE_SIZE);
    }

    public MorphAnalyzer(FileLoader loader, Map<Character,String> replaceChars, int cacheSize) throws IOException {
        tagStorage = new Tag.Storage();
        dict = new Dictionary(tagStorage, loader, replaceChars);
        knownPrefixSplitter = new KnownPrefixSplitter(loader);
        units = Lists.newArrayList(new DictionaryUnit(tagStorage, dict, true, 1.0f),
                                   new NumberUnit(tagStorage, true, 0.9f),
                                   new PunctuationUnit(tagStorage, true, 0.9f),
                                   new RomanUnit(tagStorage, false, 0.9f),
                                   new LatinUnit(tagStorage, true, 0.9f),
                                   new KnownPrefixUnit(tagStorage, dict, knownPrefixSplitter, true, 0.75f),
                                   new UnknownPrefixUnit(tagStorage, dict, true, 0.5f));
        prob = new ProbabilityEstimator(loader);
        if (cacheSize > 0) {
            cache = CacheBuilder.newBuilder().maximumSize(cacheSize).build();
        }
    }

    public Grammeme getGrammeme(String value) {
        return tagStorage.getGrammeme(value);
    }

    public Collection<Grammeme> getAllGrammemes() {
        return tagStorage.getAllGrammemes();
    }

    public Tag getTag(String tagString) {
        return tagStorage.getTag(tagString);
    }

    public Collection<Tag> getAllTags() {
        return tagStorage.getAllTags();
    }

    public List<String> normalForms(char[] buffer, int offset, int count) throws IOException {
        return normalForms(new String(buffer, offset, count));
    }

    public List<String> normalForms(String word) throws IOException {
        List<Parsed> parseds = parse(word);
        List<String> normalForms = new ArrayList<String>();
        Set<String> uniqueNormalForms = new HashSet<String>();

        for (Parsed p : parseds) {
            if (!uniqueNormalForms.contains(p.normalForm)) {
                normalForms.add(p.normalForm);
                uniqueNormalForms.add(p.normalForm);
            }
        }
        return normalForms;
    }

    public List<Tag> tag(char[] buffer, int offset, int count) throws IOException {
        return tag(new String(buffer, offset, count));
    }

    public List<Tag> tag(String word) throws IOException {
        List<Parsed> parseds = parse(word);
        List<Tag> tags = Lists.newArrayListWithCapacity(parseds.size());
        for (Parsed p : parseds) {
            tags.add(p.tag);
        }
        return tags;
    }

    public List<Parsed> parse(char[] buffer, int offset, int count) throws IOException {
        return parse(new String(buffer, offset, count));
    }

    public List<Parsed> parse(String word) throws IOException {
        List<Parsed> parseds;
        if (cache != null) {
            parseds = cache.getIfPresent(word);
            if (parseds == null) {
                parseds = parseNC(word);
                cache.put(word, parseds);
            }
            return parseds;
        }
        return parseNC(word);
    }

    private List<Parsed> parseNC(String word) throws IOException {
        List<Parsed> parseds = Lists.newArrayList();
        for (AnalyzerUnit unit : units) {
            List<Parsed> unitParseds = unit.parse(word);
            if (unitParseds == null) {
                continue;
            }

            parseds.addAll(unitParseds);
            if (unit.isTerminated() && !parseds.isEmpty()) {
                break;
            }
        }

        parseds = filterDups(parseds);
        parseds = estimate(parseds);
        Collections.sort(parseds, Collections.reverseOrder());
        return parseds;
    }

    private List<Parsed> estimate(List<Parsed> parseds) throws IOException {
        float[] newScores = new float[parseds.size()];
        float sumProbs = 0.0f, sumScores = 0.0f;
        int i = 0;
        for (Parsed parsed : parseds) {
            newScores[i] = prob.getProbability(parsed.foundWord, parsed.tag);
            sumProbs += newScores[i];
            sumScores += parsed.score;
            i++;
        }

        if (sumProbs < Parsed.EPS) {
            float k = 1.0f / sumScores;
            i = 0;
            for (Parsed parsed : parseds) {
                newScores[i] = parsed.score * k;
                i++;
            }
        }

        List<Parsed> estimatedParseds = new ArrayList<Parsed>(parseds.size());
        i = 0;
        for (Parsed parsed : parseds) {
            estimatedParseds.add(new Parsed(parsed.word,
                                            parsed.tag,
                                            parsed.normalForm,
                                            parsed.foundWord,
                                            newScores[i]));
            i++;
        }
        
        return estimatedParseds;
    }

    private List<Parsed> filterDups(List<Parsed> parseds) {
        Set<Tag> seenTags = new HashSet<Tag>();
        List<Parsed> filteredParseds = new ArrayList<Parsed>();
        for (Parsed p : parseds) {
            if (!seenTags.contains(p.tag)) {
                filteredParseds.add(p);
                seenTags.add(p.tag);
            }
        }
        return filteredParseds;
    }

    // TODO: make as part of API
    static abstract class AnalyzerUnit {
        protected final Tag.Storage tagStorage;
        protected final boolean terminate;
        protected final float score;

        public AnalyzerUnit(Tag.Storage tagStorage, boolean terminate, float score) {
            this.tagStorage = tagStorage;
            this.terminate = terminate;
            this.score = score;
        }

        public boolean isTerminated() {
            return terminate;
        }

        public abstract List<Parsed> parse(String word) throws IOException;
    };

    static class DictionaryUnit extends AnalyzerUnit {
        protected final Dictionary dict;

        public DictionaryUnit(Tag.Storage tagStorage, Dictionary dict, boolean terminate, float score) {
            super(tagStorage, terminate, score);
            this.dict = dict;
        }

        @Override
        public List<Parsed> parse(String word) throws IOException {
            return dict.parse(word.toLowerCase());
        }
    };


    static class NumberUnit extends AnalyzerUnit {
        public NumberUnit(Tag.Storage tagStorage, boolean terminate, float score) {
            super(tagStorage, terminate, score);
            this.tagStorage.newGrammeme(Lists.newArrayList("NUMB", "", "ЧИСЛО", "число"));
            this.tagStorage.newGrammeme(Lists.newArrayList("intg", "", "цел", "целое"));
            this.tagStorage.newGrammeme(Lists.newArrayList("real", "", "вещ", "вещественное"));
            this.tagStorage.newTag("NUMB,intg");
            this.tagStorage.newTag("NUMB,real");
        }

        @Override
        public List<Parsed> parse(String word) {
            Tag tag = null;
            if (Ints.tryParse(word) != null) {
                tag = tagStorage.getTag("NUMB,intg");
            }
            else if (Floats.tryParse(word) != null) {
                tag = tagStorage.getTag("NUMB,real");
            }

            if (tag != null) {
                return Lists.newArrayList(new Parsed(word, tag, word, word, score));
            }
            return null;
        }
    };

    static class RegexUnit extends AnalyzerUnit {
        protected final Pattern pattern;
        protected final String tagString;

        public RegexUnit(Tag.Storage tagStorage, String regex, String tagString, boolean terminate, float score) {
            super(tagStorage, terminate, score);
            this.pattern = Pattern.compile(regex);
            this.tagString = tagString;
        }

        @Override
        public List<Parsed> parse(String word) {
            if (pattern.matcher(word).matches()) {
                return Lists.newArrayList(new Parsed(word, tagStorage.getTag(tagString), word, word, score));
            }
            return null;
        }
    };

    static class PunctuationUnit extends RegexUnit {
        private static final String PUNCTUATION_REGEX = "\\p{Punct}+";
        
        public PunctuationUnit(Tag.Storage tagStorage, boolean terminate, float score) {
            super(tagStorage, PUNCTUATION_REGEX, "PNCT", terminate, score);
            this.tagStorage.newGrammeme(Lists.newArrayList("PNCT", "", "ЗПР", "пунктуация"));
            this.tagStorage.newTag("PNCT");
        }

    };

    static class LatinUnit extends RegexUnit {
        private static final String LATIN_REGEX = "[\\p{IsLatin}\\d\\p{Punct}]+";

        public LatinUnit(Tag.Storage tagStorage, boolean terminate, float score) {
            super(tagStorage, LATIN_REGEX, "LATN", terminate, score);
            tagStorage.newGrammeme(Lists.newArrayList("LATN", "", "ЛАТ", "латиница"));
            tagStorage.newTag("LATN");
        }
    };

    static class RomanUnit extends RegexUnit {
        private static final String ROMAN_REGEX =
            "M{0,4}" +
            "(CM|CD|D?C{0,3})" +
            "(XC|XL|L?X{0,3})" +
            "(IX|IV|V?I{0,3})";

        public RomanUnit(Tag.Storage tagStorage, boolean terminate, float score) {
            super(tagStorage, ROMAN_REGEX, "ROMN", terminate, score);
            tagStorage.newGrammeme(Lists.newArrayList("ROMN", "", "РИМ", "римские цифры"));
            tagStorage.newTag("ROMN");
        }
    };

    static abstract class PrefixedUnit extends DictionaryUnit {
        protected static final int MAX_PREFIX_LENGTH = 5;
        protected static final int MIN_REMINDER = 3;

        public PrefixedUnit(Tag.Storage tagStorage, Dictionary dict, boolean terminate, float score) {
            super(tagStorage, dict, terminate, score);
        }

        protected List<Parsed> parseWithPrefix(String word, String prefix) throws IOException {
            List<Parsed> parseds = new ArrayList<Parsed>();
            for (Parsed p : dict.parse(word.substring(prefix.length()))) {
                if (!p.tag.isProductive()) {
                    continue;
                }
                parseds.add(new Parsed(prefix + p.word,
                                       p.tag,
                                       prefix + p.normalForm,
                                       p.word,
                                       p.score * score));
            }
            return parseds;
        }
    };

    static class KnownPrefixUnit extends PrefixedUnit {
        protected final KnownPrefixSplitter knownPrefixSplitter;

        public KnownPrefixUnit(Tag.Storage tagStorage, Dictionary dict, KnownPrefixSplitter knownPrefixSplitter, boolean terminate, float score) {
            super(tagStorage, dict, terminate, score);
            this.knownPrefixSplitter = knownPrefixSplitter;
        }

        @Override
        public List<Parsed> parse(String word) throws IOException {
            word = word.toLowerCase();
            List<Parsed> parseds = new ArrayList<Parsed>();
            for (String prefix : knownPrefixSplitter.prefixes(word, MIN_REMINDER)) {
                parseds.addAll(parseWithPrefix(word, prefix));
            }
            return parseds;
        }
    };

    static class UnknownPrefixUnit extends PrefixedUnit {
        public UnknownPrefixUnit(Tag.Storage tagStorage, Dictionary dict, boolean terminate, float score) {
            super(tagStorage, dict, terminate, score);
        }

        @Override
        public List<Parsed> parse(String word) throws IOException {
            word = word.toLowerCase();
            List<Parsed> parseds = new ArrayList<Parsed>();
            int wordLength = word.length();
            for (int i = 1; i <= MAX_PREFIX_LENGTH && wordLength - i >= MIN_REMINDER; i++) {
                parseds.addAll(parseWithPrefix(word, word.substring(0, i - 1)));
            }
            return parseds;
        }
    };
}
