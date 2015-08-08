package net.uaprom.jmorphy2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Floats;


abstract class AnalyzerUnit {
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

    public abstract List<ParsedWord> parse(String word) throws IOException;

    class AnalyzerParsedWord extends ParsedWord {
        public AnalyzerParsedWord(String word, Tag tag, String normalForm, String foundWord, float score) {
            super(word, tag, normalForm, foundWord, score);
        }

        @Override
        public ParsedWord rescore(float newScore) {
            return new AnalyzerParsedWord(word, tag, normalForm, foundWord, newScore);
        }

        @Override
        public List<ParsedWord> getLexeme() {
            return Arrays.asList((ParsedWord) this);
        }

        @Override
        public String toString() {
            return String.format("<ParsedWord: \"%s\", \"%s\", \"%s\", \"%s\", %.6f, %s>", word, tag, normalForm, foundWord, score, AnalyzerUnit.this.getClass());
        }
    };

    // Concrete analyzer units

    static class DictionaryUnit extends AnalyzerUnit {
        protected final Dictionary dict;

        public DictionaryUnit(Tag.Storage tagStorage, Dictionary dict, boolean terminate, float score) {
            super(tagStorage, terminate, score);
            this.dict = dict;
        }

        @Override
        public List<ParsedWord> parse(String word) throws IOException {
            List<Dictionary.Parsed> parseds = dict.parse(word.toLowerCase());
            List<ParsedWord> parsedWords = new ArrayList<ParsedWord>();
            for (Dictionary.Parsed p : parseds) {
                parsedWords.add(new DictionaryParsedWord(p.word, p.tag, p.normalForm, p.wordForm.word, score, p.wordForm));
            }
            return parsedWords;
        }

        class DictionaryParsedWord extends AnalyzerParsedWord {
            public final WordsDAWG.WordForm wordForm;

            public DictionaryParsedWord(String word, Tag tag, String normalForm, String foundWord, float score, WordsDAWG.WordForm wordForm) {
                super(word, tag, normalForm, foundWord, score);
                this.wordForm = wordForm;
            }

            public DictionaryParsedWord(Dictionary.Parsed p, float score) {
                this(p.word, p.tag, p.normalForm, p.wordForm.word, score, p.wordForm);
            }

            @Override
            public ParsedWord rescore(float newScore) {
                return new DictionaryParsedWord(word, tag, normalForm, foundWord, newScore, wordForm);
            }

            @Override
            public List<ParsedWord> getLexeme() {
                List<Dictionary.Parsed> dictParseds = DictionaryUnit.this.dict.getLexeme(wordForm);
                List<ParsedWord> lexeme = new ArrayList<>();
                for (Dictionary.Parsed p : dictParseds) {
                    lexeme.add(new DictionaryParsedWord(p, 1.0f));
                }
                return lexeme;
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj)
                    && wordForm.equals(((DictionaryParsedWord) obj).wordForm);
            }
        }
    };

    static abstract public class PrefixedUnit extends DictionaryUnit {
        public PrefixedUnit(Tag.Storage tagStorage, Dictionary dict, boolean terminate, float score) {
            super(tagStorage, dict, terminate, score);
        }

        protected List<ParsedWord> parseWithPrefix(String word, String prefix) throws IOException {
            List<ParsedWord> parseds = new ArrayList<ParsedWord>();
            for (Dictionary.Parsed p : dict.parse(word.substring(prefix.length()))) {
                if (!p.tag.isProductive()) {
                    continue;
                }
                parseds.add(new PrefixedDictionaryParsedWord(prefix, p, score));
            }
            return parseds;
        }

        class PrefixedDictionaryParsedWord extends DictionaryParsedWord {
            private final String prefix;
            
            public PrefixedDictionaryParsedWord(String prefix, String word, Tag tag, String normalForm, String foundWord, float score, WordsDAWG.WordForm wordForm) {
                super(word, tag, normalForm, foundWord, score, wordForm);
                this.prefix = prefix;
            }

            public PrefixedDictionaryParsedWord(String prefix, Dictionary.Parsed p, float score) {
                this(prefix, prefix + p.word, p.tag, prefix + p.normalForm, p.wordForm.word, score, p.wordForm);
            }

            @Override
            public ParsedWord rescore(float newScore) {
                return new PrefixedDictionaryParsedWord(prefix, word, tag, normalForm, foundWord, newScore, wordForm);
            }

            @Override
            public List<ParsedWord> getLexeme() {
                List<Dictionary.Parsed> dictParseds = PrefixedUnit.this.dict.getLexeme(wordForm);
                List<ParsedWord> lexeme = new ArrayList<>();
                for (Dictionary.Parsed p : dictParseds) {
                    lexeme.add(new PrefixedDictionaryParsedWord(prefix, p, 1.0f));
                }
                return lexeme;
            }
        };
    };

    static public class KnownPrefixUnit extends PrefixedUnit {
        protected static final int DEFAULT_MIN_REMINDER = 3;

        protected final KnownPrefixSplitter knownPrefixSplitter;
        protected final int minReminder;

        public KnownPrefixUnit(Tag.Storage tagStorage, Dictionary dict, KnownPrefixSplitter knownPrefixSplitter, boolean terminate, float score) {
            this(tagStorage, dict, knownPrefixSplitter, terminate, score, DEFAULT_MIN_REMINDER);
        }

        public KnownPrefixUnit(Tag.Storage tagStorage, Dictionary dict, KnownPrefixSplitter knownPrefixSplitter, boolean terminate, float score, int minReminder) {
            super(tagStorage, dict, terminate, score);
            this.knownPrefixSplitter = knownPrefixSplitter;
            this.minReminder = minReminder;
        }

        @Override
        public List<ParsedWord> parse(String word) throws IOException {
            word = word.toLowerCase();
            List<ParsedWord> parseds = new ArrayList<>();
            for (String prefix : knownPrefixSplitter.prefixes(word, minReminder)) {
                parseds.addAll(parseWithPrefix(word, prefix));
            }
            return parseds;
        }
    };

    static public class UnknownPrefixUnit extends PrefixedUnit {
        protected static final int DEFAULT_MAX_PREFIX_LENGTH = 5;
        protected static final int DEFAULT_MIN_REMINDER = 3;

        protected final int maxPrefixLength;
        protected final int minReminder;

        public UnknownPrefixUnit(Tag.Storage tagStorage, Dictionary dict, boolean terminate, float score) {
            this(tagStorage, dict, terminate, score, DEFAULT_MAX_PREFIX_LENGTH, DEFAULT_MIN_REMINDER);
        }

        public UnknownPrefixUnit(Tag.Storage tagStorage, Dictionary dict, boolean terminate, float score, int maxPrefixLength, int minReminder) {
            super(tagStorage, dict, terminate, score);
            this.maxPrefixLength = maxPrefixLength;
            this.minReminder = minReminder;
        }

        @Override
        public List<ParsedWord> parse(String word) throws IOException {
            word = word.toLowerCase();
            List<ParsedWord> parseds = new ArrayList<ParsedWord>();
            int wordLength = word.length();
            for (int i = 1; i <= maxPrefixLength && wordLength - i >= minReminder; i++) {
                parseds.addAll(parseWithPrefix(word, word.substring(0, i - 1)));
            }
            return parseds;
        }
    };

    static public class NumberUnit extends AnalyzerUnit {
        public NumberUnit(Tag.Storage tagStorage, boolean terminate, float score) {
            super(tagStorage, terminate, score);
            this.tagStorage.newGrammeme(Lists.newArrayList("NUMB", "", "ЧИСЛО", "число"));
            this.tagStorage.newGrammeme(Lists.newArrayList("intg", "", "цел", "целое"));
            this.tagStorage.newGrammeme(Lists.newArrayList("real", "", "вещ", "вещественное"));
            this.tagStorage.newTag("NUMB,intg");
            this.tagStorage.newTag("NUMB,real");
        }

        @Override
        public List<ParsedWord> parse(String word) {
            Tag tag = null;
            if (Ints.tryParse(word) != null) {
                tag = tagStorage.getTag("NUMB,intg");
            }
            else if (Floats.tryParse(word) != null) {
                tag = tagStorage.getTag("NUMB,real");
            }

            if (tag != null) {
                List<ParsedWord> parseds = new ArrayList<>();
                parseds.add(new AnalyzerParsedWord(word, tag, word, word, score));
                return parseds;
            }
            return null;
        }
    };

    static public class RegexUnit extends AnalyzerUnit {
        protected final Pattern pattern;
        protected final String tagString;

        public RegexUnit(Tag.Storage tagStorage, String regex, String tagString, boolean terminate, float score) {
            super(tagStorage, terminate, score);
            this.pattern = Pattern.compile(regex);
            this.tagString = tagString;
        }

        @Override
        public List<ParsedWord> parse(String word) {
            if (pattern.matcher(word).matches()) {
                List<ParsedWord> parseds = new ArrayList<>();
                parseds.add(new AnalyzerParsedWord(word, tagStorage.getTag(tagString), word, word, score));
                return parseds;
            }
            return null;
        }
    };

    static public class PunctuationUnit extends RegexUnit {
        private static final String PUNCTUATION_REGEX = "\\p{Punct}+";
        
        public PunctuationUnit(Tag.Storage tagStorage, boolean terminate, float score) {
            super(tagStorage, PUNCTUATION_REGEX, "PNCT", terminate, score);
            this.tagStorage.newGrammeme(Lists.newArrayList("PNCT", "", "ЗПР", "пунктуация"));
            this.tagStorage.newTag("PNCT");
        }

    };

    static public class LatinUnit extends RegexUnit {
        private static final String LATIN_REGEX = "[\\p{IsLatin}\\d\\p{Punct}]+";

        public LatinUnit(Tag.Storage tagStorage, boolean terminate, float score) {
            super(tagStorage, LATIN_REGEX, "LATN", terminate, score);
            tagStorage.newGrammeme(Lists.newArrayList("LATN", "", "ЛАТ", "латиница"));
            tagStorage.newTag("LATN");
        }
    };

    static public class RomanUnit extends RegexUnit {
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

    static public class UnknownWordUnit extends AnalyzerUnit {
        public UnknownWordUnit(Tag.Storage tagStorage, boolean terminate, float score) {
            super(tagStorage, terminate, score);
            tagStorage.newGrammeme(Lists.newArrayList("UNKN", "", "НЕИЗВ", "неизвестное"));
            tagStorage.newTag("UNKN");
        }

        @Override
        public List<ParsedWord> parse(String word) {
            List<ParsedWord> parseds = new ArrayList<>();
            parseds.add(new AnalyzerParsedWord(word, tagStorage.getTag("UNKN"), word, word, score));
            return parseds;
        }
    };
}
