package company.evo.jmorphy2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

import company.evo.jmorphy2.units.*;


public class MorphAnalyzer {
    private final Tag.Storage tagStorage;
    private final List<AnalyzerUnit> units;
    private final ProbabilityEstimator prob;
    private final Cache<String,List<ParsedWord>> cache;

    enum Lang { RU, UK };

    public static class Builder {
        // private static final String ENV_DICT_PATH = "PYMORPHY2_DICT_PATH";
        private static final String DICT_PATH_VAR = "dictPath";
        public static final int DEFAULT_CACHE_SIZE = 10000;

        private String dictPath;
        private FileLoader loader;
        private Map<Character,String> charSubstitutes;
        private Lang lang = Lang.RU;
        private List<AnalyzerUnit.Builder> unitBuilders;
        private int cacheSize = DEFAULT_CACHE_SIZE;
        private Cache<String,List<ParsedWord>> cache = null;

        public Builder dictPath(String path) {
            this.dictPath = path;
            return this;
        }

        public Builder fileLoader(FileLoader loader) {
            this.loader = loader;
            return this;
        }

        public Builder charSubstitutes(Map<Character,String> charSubstitutes) {
            this.charSubstitutes = charSubstitutes;
            return this;
        }

        public Builder cacheSize(int size) {
            this.cacheSize = size;
            return this;
        }

        public MorphAnalyzer build() throws IOException {
            Tag.Storage tagStorage = new Tag.Storage();
            if (loader == null) {
                if (dictPath == null) {
                    dictPath = System.getProperty(DICT_PATH_VAR);
                }
                loader = new FSFileLoader(dictPath);
            }
            if (unitBuilders == null) {
                Dictionary.Builder dictBuilder = new Dictionary.Builder(loader);
                String langCode = dictBuilder.build(tagStorage).getMeta().languageCode.toUpperCase();
                lang = Lang.valueOf(langCode);
                Set<String> knownPrefixes = null;
                if (lang != null) {
                    knownPrefixes = Resources.getKnownPrefixes(lang);
                    if (charSubstitutes == null) {
                        charSubstitutes = Resources.getCharSubstitutes(lang);
                    }
                }
                AnalyzerUnit.Builder dictUnitBuilder = new DictionaryUnit.Builder(dictBuilder, true, 1.0f)
                    .charSubstitutes(charSubstitutes);
                unitBuilders = new ArrayList<>();
                unitBuilders.add(dictUnitBuilder);
                unitBuilders.add(new NumberUnit.Builder(true, 0.9f));
                unitBuilders.add(new PunctuationUnit.Builder(true, 0.9f));
                unitBuilders.add(new RomanUnit.Builder(false, 0.9f));
                unitBuilders.add(new LatinUnit.Builder(true, 0.9f));
                if (knownPrefixes != null) {
                    unitBuilders.add(new KnownPrefixUnit.Builder(dictUnitBuilder, knownPrefixes, true, 0.75f));
                }
                unitBuilders.add(new UnknownPrefixUnit.Builder(dictUnitBuilder, true, 0.5f));
                unitBuilders.add(new KnownSuffixUnit.Builder(dictBuilder, true, 0.5f)
                                 .charSubstitutes(charSubstitutes));
                unitBuilders.add(new UnknownUnit.Builder(true, 1.0f));
            }
            List<AnalyzerUnit> units = new ArrayList<>();
            Dictionary.Meta dictMeta = null;
            for (AnalyzerUnit.Builder unitBuilder : unitBuilders) {
                AnalyzerUnit unit = unitBuilder.build(tagStorage);
                if (unit instanceof DictionaryUnit) {
                    dictMeta = ((DictionaryUnit) unit).getDict().getMeta();
                }
                units.add(unit);
            }
            ProbabilityEstimator prob = null;
            if (dictMeta != null && dictMeta.ptw) {
                prob = new ProbabilityEstimator(loader);
            }
            if (cacheSize > 0) {
                cache = CacheBuilder.newBuilder().maximumSize(cacheSize).build();
            }
            return new MorphAnalyzer(tagStorage, units, prob, cache);
        }
    };

    private MorphAnalyzer(Tag.Storage tagStorage,
                          List<AnalyzerUnit> units,
                          ProbabilityEstimator prob,
                          Cache<String,List<ParsedWord>> cache) throws IOException {
        this.tagStorage = tagStorage;
        this.units = units;
        this.prob = prob;
        this.cache = cache;
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
        List<Tag> tags = new ArrayList<>(parseds.size());
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
        String wordLower = word.toLowerCase();
        List<ParsedWord> parseds = new ArrayList<>();
        for (AnalyzerUnit unit : units) {
            List<ParsedWord> unitParseds = unit.parse(word, wordLower);
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
        if (prob == null) {
            return parseds;
        }
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
        Set<ParsedWord.Unique> seenParseds = new HashSet<>();
        List<ParsedWord> filteredParseds = new ArrayList<ParsedWord>();
        for (ParsedWord p : parseds) {
            ParsedWord.Unique u = p.toUnique();
            if (!seenParseds.contains(u)) {
                filteredParseds.add(p);
                seenParseds.add(u);
            }
        }
        return filteredParseds;
    }
}
