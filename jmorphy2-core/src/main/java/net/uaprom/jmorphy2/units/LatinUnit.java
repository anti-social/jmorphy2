package net.uaprom.jmorphy2.units;

import com.google.common.collect.Lists;

import net.uaprom.jmorphy2.Tag;


public class LatinUnit extends RegexUnit {
    private static final String LATIN_REGEX = "[\\p{IsLatin}\\d\\p{Punct}]+";

    public LatinUnit(Tag.Storage tagStorage, boolean terminate, float score) {
        super(tagStorage, LATIN_REGEX, "LATN", terminate, score);
    }

    public static class Builder extends AnalyzerUnit.Builder {
        public Builder(boolean terminate, float score) {
            super(terminate, score);
        }

        @Override
        public AnalyzerUnit build(Tag.Storage tagStorage) {
            tagStorage.newGrammeme(Lists.newArrayList("LATN", "", "ЛАТ", "латиница"));
            tagStorage.newTag("LATN");
            return new LatinUnit(tagStorage, terminate, score);
        }
    }
}
