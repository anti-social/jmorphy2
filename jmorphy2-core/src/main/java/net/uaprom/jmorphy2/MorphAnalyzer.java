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
import java.text.Normalizer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;


public class MorphAnalyzer {
    private final Tag.Storage tagStorage;
    private final Dictionary dict;
    private final KnownPrefixSplitter knownPrefixSplitter;
    private final List<AnalyzerUnit> units;
    private final ProbabilityEstimator prob;
    private Cache<String,List<ParsedWord>> cache = null;

    // private static final String ENV_DICT_PATH = "PYMORPHY2_DICT_PATH";
    private static final String DICT_PATH_VAR = "dictPath";

    public static final int DEFAULT_CACHE_SIZE = 10000;

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
        units = Lists.newArrayList(new AnalyzerUnit.DictionaryUnit(tagStorage, dict, true, 1.0f),
                                   new AnalyzerUnit.NumberUnit(tagStorage, true, 0.9f),
                                   new AnalyzerUnit.PunctuationUnit(tagStorage, true, 0.9f),
                                   new AnalyzerUnit.RomanUnit(tagStorage, false, 0.9f),
                                   new AnalyzerUnit.LatinUnit(tagStorage, true, 0.9f),
                                   new AnalyzerUnit.KnownPrefixUnit(tagStorage, dict, knownPrefixSplitter, true, 0.75f),
                                   new AnalyzerUnit.UnknownPrefixUnit(tagStorage, dict, true, 0.5f),
                                   new AnalyzerUnit.UnknownWordUnit(tagStorage, true, 1.0f));
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
        List<ParsedWord> parseds = parse(word);
        List<String> normalForms = new ArrayList<String>();
        Set<String> uniqueNormalForms = new HashSet<String>();

        for (ParsedWord p : parseds) {
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
        List<ParsedWord> parseds = parse(word);
        List<Tag> tags = Lists.newArrayListWithCapacity(parseds.size());
        for (ParsedWord p : parseds) {
            tags.add(p.tag);
        }
        return tags;
    }

    public List<ParsedWord> parse(char[] buffer, int offset, int count) throws IOException {
        return parse(new String(buffer, offset, count));
    }

    public List<ParsedWord> parse(String word) throws IOException {
        List<ParsedWord> parseds;
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

    private List<ParsedWord> parseNC(String word) throws IOException {
        List<ParsedWord> parseds = Lists.newArrayList();
        for (AnalyzerUnit unit : units) {
            List<ParsedWord> unitParseds = unit.parse(word);
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

    private List<ParsedWord> estimate(List<ParsedWord> parseds) throws IOException {
        float[] newScores = new float[parseds.size()];
        float sumProbs = 0.0f, sumScores = 0.0f;
        int i = 0;
        for (ParsedWord parsed : parseds) {
            newScores[i] = prob.getProbability(parsed.foundWord, parsed.tag);
            sumProbs += newScores[i];
            sumScores += parsed.score;
            i++;
        }

        if (sumProbs < ParsedWord.EPS) {
            float k = 1.0f / sumScores;
            i = 0;
            for (ParsedWord parsed : parseds) {
                newScores[i] = parsed.score * k;
                i++;
            }
        }

        List<ParsedWord> estimatedParseds = new ArrayList<ParsedWord>(parseds.size());
        i = 0;
        for (ParsedWord parsed : parseds) {
            estimatedParseds.add(parsed.rescore(newScores[i]));
            i++;
        }
        
        return estimatedParseds;
    }

    private List<ParsedWord> filterDups(List<ParsedWord> parseds) {
        Set<Tag> seenTags = new HashSet<Tag>();
        List<ParsedWord> filteredParseds = new ArrayList<ParsedWord>();
        for (ParsedWord p : parseds) {
            if (!seenTags.contains(p.tag)) {
                filteredParseds.add(p);
                seenTags.add(p.tag);
            }
        }
        return filteredParseds;
    }
}
