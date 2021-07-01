package company.evo.jmorphy2.units;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import company.evo.jmorphy2.ParsedWord;
import company.evo.jmorphy2.Tag;


public class KnownPrefixUnit extends PrefixedUnit {
    private final int minReminder;
    private final Set<String> prefixes;

    private KnownPrefixUnit(
        Tag.Storage tagStorage,
        AnalyzerUnit unit,
        Set<String> prefixes,
        int minReminder,
        boolean terminate,
        float score
    ) {
        super(tagStorage, unit, terminate, score);
        this.minReminder = minReminder;
        this.prefixes = prefixes;
    }

    public static class Builder extends AnalyzerUnit.Builder {
        private static final int DEFAULT_MIN_REMINDER = 3;

        private final AnalyzerUnit.Builder unit;
        private final Set<String> prefixes;
        private int minReminder = DEFAULT_MIN_REMINDER;

        public Builder(AnalyzerUnit.Builder unit,
                       Set<String> prefixes,
                       boolean terminate,
                       float score) {
            super(terminate, score);
            this.unit = unit;
            this.prefixes = prefixes;
        }

        public Builder minReminder(int minReminder) {
            this.minReminder = minReminder;
            this.cachedUnit = null;
            return this;
        }

        @Override
        protected AnalyzerUnit newAnalyzerUnit(Tag.Storage tagStorage) throws IOException {
            return new KnownPrefixUnit(tagStorage,
                                       unit.build(tagStorage),
                                       prefixes,
                                       minReminder,
                                       terminate,
                                       score);
        }
    }

    @Override
    public List<ParsedWord> parse(String word, String wordLower) {
        List<ParsedWord> parseds = new ArrayList<>();
        int wordLen = word.length();
        for (int i = 1; wordLen - i >= minReminder; i++) {
            String prefix = wordLower.substring(0, i);
            if (prefixes.contains(prefix)) {
                parseds.addAll(parseWithPrefix(word, wordLower, prefix));
            }
        }
        return parseds;
    }
};
