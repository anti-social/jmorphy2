package company.evo.jmorphy2.units;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import company.evo.jmorphy2.ParsedWord;
import company.evo.jmorphy2.Tag;


public abstract class AnalyzerUnit {
    protected final Tag.Storage tagStorage;
    protected final boolean terminate;
    protected final float score;

    public static abstract class Builder {
        protected final boolean terminate;
        protected final float score;
        protected AnalyzerUnit cachedUnit;

        protected Builder(boolean terminate, float score) {
            this.terminate = terminate;
            this.score = score;
        }

        protected abstract AnalyzerUnit newAnalyzerUnit(Tag.Storage tagStorage) throws IOException;

        public AnalyzerUnit build(Tag.Storage tagStorage) throws IOException {
            if (cachedUnit == null) {
                cachedUnit = newAnalyzerUnit(tagStorage);
            }
            return cachedUnit;
        }
    }

    protected AnalyzerUnit(Tag.Storage tagStorage, boolean terminate, float score) {
        this.tagStorage = tagStorage;
        this.terminate = terminate;
        this.score = score;
    }

    public boolean isTerminated() {
        return terminate;
    }

    public abstract List<ParsedWord> parse(String word, String wordLower);

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
            return Collections.singletonList(this);
        }

        @Override
        public String toString() {
            return String.format(
                "<ParsedWord: \"%s\", \"%s\", \"%s\", \"%s\", %.6f, %s>",
                word, tag, normalForm, foundWord, score, AnalyzerUnit.this.getClass()
            );
        }
    }
}
