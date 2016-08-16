package net.uaprom.jmorphy2.units;

import com.google.common.collect.Lists;

import net.uaprom.jmorphy2.Tag;


public class PunctuationUnit extends RegexUnit {
    private static final String PUNCTUATION_REGEX = "\\p{Punct}+";
    
    private PunctuationUnit(Tag.Storage tagStorage, boolean terminate, float score) {
        super(tagStorage, PUNCTUATION_REGEX, "PNCT", terminate, score);
    }

    public static class Builder extends AnalyzerUnit.Builder {
        public Builder(boolean terminate, float score) {
            super(terminate, score);
        }

        @Override
        protected AnalyzerUnit newAnalyzerUnit(Tag.Storage tagStorage) {
            tagStorage.newGrammeme(Lists.newArrayList("PNCT", "", "ЗПР", "пунктуация"));
            tagStorage.newTag("PNCT");
            return new PunctuationUnit(tagStorage, terminate, score);
        }
    }
}
