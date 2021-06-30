package company.evo.jmorphy2.units;

import java.util.List;

import company.evo.jmorphy2.Tag;


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
            tagStorage.newGrammeme(List.of("PNCT", "", "ЗПР", "пунктуация"));
            tagStorage.newTag("PNCT");
            return new PunctuationUnit(tagStorage, terminate, score);
        }
    }
}
