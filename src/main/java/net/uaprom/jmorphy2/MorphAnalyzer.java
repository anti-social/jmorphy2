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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


public class MorphAnalyzer {
    private final Dictionary dict;
    private final ProbabilityEstimator prob;
    private final KnownPrefixSplitter knownPrefixSplitter;
    private Cache<String,List<Parsed>> cache = null;

    // private static final String ENV_DICT_PATH = "PYMORPHY2_DICT_PATH";
    private static final String DICT_PATH_VAR = "dictPath";

    private static final float KNOWN_PREFIX_DECAY = 0.75f;
    private static final float UNKNOWN_PREFIX_DECAY = 0.5f;
    private static final int MAX_PREFIX_LENGTH = 5;
    private static final int MIN_REMINDER = 3;

    private static final int DEFAULT_CACHE_SIZE = 10000;

    private static final Logger logger = LoggerFactory.getLogger(MorphAnalyzer.class);

    public static abstract class Loader {
        public abstract InputStream getStream(String filename) throws IOException;
    }

    public static class FileSystemLoader extends Loader {
        private String path;

        public FileSystemLoader(String path) {
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
        this(new FileSystemLoader(path), replaceChars, DEFAULT_CACHE_SIZE);
    }

    public MorphAnalyzer(String path, Map<Character,String> replaceChars, int cacheSize) throws IOException {
        this(new FileSystemLoader(path), replaceChars, cacheSize);
    }

    public MorphAnalyzer(Loader loader, Map<Character,String> replaceChars) throws IOException {
        this(loader, replaceChars, DEFAULT_CACHE_SIZE);
    }

    public MorphAnalyzer(Loader loader, Map<Character,String> replaceChars, int cacheSize) throws IOException {
        dict = new Dictionary(loader, replaceChars);
        prob = new ProbabilityEstimator(loader);
        knownPrefixSplitter = new KnownPrefixSplitter(loader);
        if (cacheSize > 0) {
            cache = CacheBuilder.newBuilder().maximumSize(cacheSize).build();
        }
    }

    public Tag getTag(String tagString) {
        return dict.getTag(tagString);
    }

    public Grammeme getGrammeme(String value) {
        return dict.getGrammeme(value);
    }

    public Collection<Grammeme> getAllGrammemes() {
        return dict.getAllGrammemes();
    }

    public List<String> getNormalForms(String word) throws IOException {
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
        String wordLower = word.toLowerCase();
        List<Parsed> parseds = parseDict(wordLower);

        if (parseds.isEmpty()) {
            parseds.addAll(parseKnownPrefix(wordLower));
        }
        if (parseds.isEmpty()) {
            parseds.addAll(parseUnknownPrefix(wordLower));
        }

        parseds = filterDups(parseds);
        parseds = estimate(parseds);
        Collections.sort(parseds, Collections.reverseOrder());
        return parseds;
    }

    private List<Parsed> parseDict(String word) throws IOException {
        return dict.parse(word);
    }

    private List<Parsed> parseKnownPrefix(String word) throws IOException {
        List<Parsed> parseds = new ArrayList<Parsed>();
        for (String prefix : knownPrefixSplitter.prefixes(word, MIN_REMINDER)) {
            parseds.addAll(parseWithPrefix(word, prefix, KNOWN_PREFIX_DECAY));
        }
        return parseds;
    }

    private List<Parsed> parseUnknownPrefix(String word) throws IOException {
        List<Parsed> parseds = new ArrayList<Parsed>();
        int wordLength = word.length();
        for (int i = 1; i <= MAX_PREFIX_LENGTH && wordLength - i >= MIN_REMINDER; i++) {
            parseds.addAll(parseWithPrefix(word, word.substring(0, i - 1), UNKNOWN_PREFIX_DECAY));
        }
        return parseds;
    }

    private List<Parsed> parseWithPrefix(String word, String prefix, float decay) throws IOException {
        List<Parsed> parseds = new ArrayList<Parsed>();
        for (Parsed p : dict.parse(word.substring(prefix.length()))) {
            if (!p.tag.isProductive()) {
                continue;
            }
            parseds.add(new Parsed(prefix + p.word,
                                   p.tag,
                                   prefix + p.normalForm,
                                   p.word,
                                   p.score * decay));
        }
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
}
