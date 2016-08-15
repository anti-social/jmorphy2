package net.uaprom.jmorphy2.units;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.uaprom.jmorphy2.ParsedWord;
import net.uaprom.jmorphy2.Tag;


public abstract class AnalyzerUnit {
    protected final Tag.Storage tagStorage;
    protected final boolean terminate;
    protected final float score;

    public static abstract class Builder {
        protected final boolean terminate;
        protected final float score;
        
        protected Builder(boolean terminate, float score) {
            this.terminate = terminate;
            this.score = score;
        }

        public abstract AnalyzerUnit build(Tag.Storage tagStorage) throws IOException;
    }
    
    protected AnalyzerUnit(Tag.Storage tagStorage, boolean terminate, float score) {
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
            return String.format("<ParsedWord: \"%s\", \"%s\", \"%s\", \"%s\", %.6f, %s>",
                                 word, tag, normalForm, foundWord, score, AnalyzerUnit.this.getClass());
        }
    }
}
