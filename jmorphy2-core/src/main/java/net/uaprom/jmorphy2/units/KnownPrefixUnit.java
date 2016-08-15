package net.uaprom.jmorphy2.units;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import net.uaprom.jmorphy2.ParsedWord;
import net.uaprom.jmorphy2.Tag;


public class KnownPrefixUnit extends PrefixedUnit {
    private final int minReminder;
    private final Set<String> prefixes;

    private KnownPrefixUnit(Tag.Storage tagStorage,
                            AnalyzerUnit unit,
                            Set<String> prefixes,
                            int minReminder,
                            boolean terminate,
                            float score) {
        super(tagStorage, unit, terminate, score);
        this.minReminder = minReminder;
        this.prefixes = prefixes;
    }

    public static class Builder extends AnalyzerUnit.Builder {
        private static final int DEFAULT_MIN_REMINDER = 3;

        private AnalyzerUnit.Builder unit;
        private Set<String> prefixes;
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
            return this;
        }

        @Override
        public AnalyzerUnit build(Tag.Storage tagStorage) throws IOException {
            return new KnownPrefixUnit(tagStorage,
                                       unit.build(tagStorage),
                                       prefixes,
                                       minReminder,
                                       terminate,
                                       score);
        }
    }

    @Override
    public List<ParsedWord> parse(String word) throws IOException {
        word = word.toLowerCase();
        List<ParsedWord> parseds = new ArrayList<>();
        int wordLength = word.length();
        for (int i = 1; wordLength - i >= minReminder; i++) {
            String prefix = word.substring(0, i);
            System.out.println(prefix);
            System.out.println(prefixes.contains(prefix));
            if (prefixes.contains(prefix)) {
                parseds.addAll(parseWithPrefix(word, prefix));
            }
        }
        return parseds;
    }
};
