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
    private Dictionary dict;
    private ProbabilityEstimator prob;
    private Cache<String,List<Parsed>> cache = null;

    // private static final String ENV_DICT_PATH = "PYMORPHY2_DICT_PATH";
    private static final String DICT_PATH_VAR = "dictPath";

    private static final int MAX_PREFIX_LENGTH = 6;
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
        List<Parsed> parseds = dict.parse(word);
        int wordLength = word.length();
        int i = 1;
        while (parseds.isEmpty()) {
            if (i > MAX_PREFIX_LENGTH || wordLength - i < MIN_REMINDER) {
                break;
            }
            parseds = dict.parse(word.substring(i));
            i++;
        }
        if (i > 1) {
            String prefix = word.substring(0, i - 1);
            List<Parsed> parsedsWithPrefix = new ArrayList<Parsed>();
            for (Parsed parsed : parseds) {
                parsedsWithPrefix.add(new Parsed(prefix + parsed.word,
                                                 parsed.tag,
                                                 prefix + parsed.normalForm,
                                                 parsed.score));
            }
            parseds = parsedsWithPrefix;
        }

        parseds = estimate(parseds);
        Collections.sort(parseds, Collections.reverseOrder());
        return parseds;
    }

    private List<Parsed> estimate(List<Parsed> parseds) throws IOException {
        float[] newScores = new float[parseds.size()];
        float sumProbs = 0.0f, sumScores = 0.0f;
        int i = 0;
        for (Parsed parsed : parseds) {
            newScores[i] = prob.getProbability(parsed.word, parsed.tag);
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
                                            newScores[i]));
            i++;
        }
        
        return estimatedParseds;
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
}
