package company.evo.jmorphy2.units;

import java.util.ArrayList;

import company.evo.jmorphy2.Tag;


public class RomanUnit extends RegexUnit {
    private static final String ROMAN_REGEX =
        "M{0,4}" +
        "(CM|CD|D?C{0,3})" +
        "(XC|XL|L?X{0,3})" +
        "(IX|IV|V?I{0,3})";

    private RomanUnit(Tag.Storage tagStorage, boolean terminate, float score) {
        super(tagStorage, ROMAN_REGEX, "ROMN", terminate, score);
    }

    public static class Builder extends AnalyzerUnit.Builder {
        public Builder(boolean terminate, float score) {
            super(terminate, score);
        }

        @Override
        protected AnalyzerUnit newAnalyzerUnit(Tag.Storage tagStorage) {
            tagStorage.newGrammeme(new ArrayList<>() {{
                add("ROMN");
                add("");
                add("РИМ");
                add("римские цифры");
            }});
            tagStorage.newTag("ROMN");
            return new RomanUnit(tagStorage, terminate, score);
        }
    }
}
