package company.evo.jmorphy2.units;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import company.evo.jmorphy2.ParsedWord;
import company.evo.jmorphy2.Tag;


public class UnknownPrefixUnit extends PrefixedUnit {
    private final int maxPrefixLength;
    private final int minReminder;

    private UnknownPrefixUnit(Tag.Storage tagStorage,
                              AnalyzerUnit unit,
                              int maxPrefixLength,
                              int minReminder,
                              boolean terminate,
                              float score) {
        super(tagStorage, unit, terminate, score);
        this.maxPrefixLength = maxPrefixLength;
        this.minReminder = minReminder;
    }

    public static class Builder extends AnalyzerUnit.Builder {
        private static final int DEFAULT_MAX_PREFIX_LENGTH = 5;
        private static final int DEFAULT_MIN_REMINDER = 3;

        private final AnalyzerUnit.Builder unit;
        private int minReminder = DEFAULT_MIN_REMINDER;
        private int maxPrefixLength = DEFAULT_MAX_PREFIX_LENGTH;

        public Builder(AnalyzerUnit.Builder unit,
                       boolean terminate,
                       float score) {
            super(terminate, score);
            this.unit = unit;
        }

        public Builder maxPrefixLength(int maxPrefixLength) {
            this.maxPrefixLength = maxPrefixLength;
            this.cachedUnit = null;
            return this;
        }

        public Builder minReminder(int minReminder) {
            this.minReminder = minReminder;
            this.cachedUnit = null;
            return this;
        }

        @Override
        protected AnalyzerUnit newAnalyzerUnit(Tag.Storage tagStorage) throws IOException {
            return new UnknownPrefixUnit(tagStorage,
                                         unit.build(tagStorage),
                                         maxPrefixLength,
                                         minReminder,
                                         terminate,
                                         score);
        }
    }

    @Override
    public List<ParsedWord> parse(String word, String wordLower) {
        List<ParsedWord> parseds = new ArrayList<>();
        int wordLen = word.length();
        for (int i = 1; i <= maxPrefixLength && wordLen - i >= minReminder; i++) {
            String prefix = wordLower.substring(0, i);
            parseds.addAll(parseWithPrefix(word, wordLower, prefix));
        }
        return parseds;
    }
}
