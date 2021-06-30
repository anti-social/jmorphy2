package company.evo.jmorphy2;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;

import company.evo.jmorphy2.units.*;


public class MorphAnalyzer {
    private final Tag.Storage tagStorage;
    private final List<AnalyzerUnit> units;
    private final ProbabilityEstimator prob;

    public static class Builder<T extends Builder<T>> {
        // private static final String ENV_DICT_PATH = "PYMORPHY2_DICT_PATH";
        private static final String DICT_PATH_VAR = "dictPath";
        public static final int DEFAULT_CACHE_SIZE = 0;

        protected Tag.Storage tagStorage = new Tag.Storage();

        private String dictPath;
        private FileLoader loader;
        private Map<Character,String> charSubstitutes;
        private List<AnalyzerUnit.Builder> unitBuilders;

        @SuppressWarnings("unchecked")
        protected final T self() {
            return (T) this;
        }

        public final T dictPath(String path) {
            this.dictPath = path;
            return self();
        }

        public final T fileLoader(FileLoader loader) {
            this.loader = loader;
            return self();
        }

        public final T charSubstitutes(Map<Character,String> charSubstitutes) {
            this.charSubstitutes = charSubstitutes;
            return self();
        }

        protected Units prepare() throws IOException {
            if (loader == null) {
                if (dictPath == null) {
                    dictPath = System.getProperty(DICT_PATH_VAR);
                }
                loader = new FSFileLoader(dictPath);
            }

            if (unitBuilders == null) {
                Dictionary.Builder dictBuilder = new Dictionary.Builder(loader);
                String langCode = dictBuilder.build(tagStorage).getMeta().languageCode.toUpperCase();
                Set<String> knownPrefixes = Resources.getKnownPrefixes(langCode);
                if (charSubstitutes == null) {
                    charSubstitutes = Resources.getCharSubstitutes(langCode);
                }
                AnalyzerUnit.Builder dictUnitBuilder = new DictionaryUnit.Builder(dictBuilder, true, 1.0f)
                    .charSubstitutes(charSubstitutes);
                unitBuilders = new ArrayList<>();
                unitBuilders.add(dictUnitBuilder);
                unitBuilders.add(new NumberUnit.Builder(true, 0.9f));
                unitBuilders.add(new PunctuationUnit.Builder(true, 0.9f));
                unitBuilders.add(new RomanUnit.Builder(false, 0.9f));
                unitBuilders.add(new LatinUnit.Builder(true, 0.9f));
                if (!knownPrefixes.isEmpty()) {
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

            ProbabilityEstimator probabilityEstimator = null;
            if (dictMeta != null && dictMeta.ptw) {
                probabilityEstimator = new ProbabilityEstimator(loader);
            }

            return new Units(units, probabilityEstimator);
        }

        public MorphAnalyzer build() throws IOException {
            var prepared = prepare();
            return new MorphAnalyzer(tagStorage, prepared.units, prepared.probabilityEstimator);
        }
    };

    public static class Units {
        public final List<AnalyzerUnit> units;
        public final ProbabilityEstimator probabilityEstimator;

        Units(List<AnalyzerUnit> units, ProbabilityEstimator probabilityEstimator) {
            this.units = units;
            this.probabilityEstimator = probabilityEstimator;
        }
    }

    protected MorphAnalyzer(
        Tag.Storage tagStorage,
        List<AnalyzerUnit> units,
        ProbabilityEstimator prob
    ) {
        this.tagStorage = tagStorage;
        this.units = units;
        this.prob = prob;
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

    public List<String> normalForms(char[] buffer, int offset, int count) {
        return normalForms(new String(buffer, offset, count));
    }

    public List<String> normalForms(String word) {
        List<ParsedWord> parseds = parse(word);
        List<String> normalForms = new ArrayList<>();
        Set<String> uniqueNormalForms = new HashSet<>();

        for (ParsedWord p : parseds) {
            if (!uniqueNormalForms.contains(p.normalForm)) {
                normalForms.add(p.normalForm);
                uniqueNormalForms.add(p.normalForm);
            }
        }
        return normalForms;
    }

    public List<Tag> tag(char[] buffer, int offset, int count) {
        return tag(new String(buffer, offset, count));
    }

    public List<Tag> tag(String word) {
        List<ParsedWord> parseds = parse(word);
        List<Tag> tags = new ArrayList<>(parseds.size());
        for (ParsedWord p : parseds) {
            tags.add(p.tag);
        }
        return tags;
    }

    public List<ParsedWord> parse(char[] buffer, int offset, int count) {
        return parse(new String(buffer, offset, count));
    }

    public List<ParsedWord> parse(String word) {
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
        parseds.sort(Collections.reverseOrder());
        return parseds;
    }

    private List<ParsedWord> estimate(List<ParsedWord> parseds) {
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
        List<ParsedWord> filteredParseds = new ArrayList<>();
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
