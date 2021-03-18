package company.evo.jmorphy2.units;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;

import company.evo.jmorphy2.ParsedWord;
import company.evo.jmorphy2.Tag;


public class NumberUnit extends AnalyzerUnit {
    private NumberUnit(Tag.Storage tagStorage, boolean terminate, float score) {
        super(tagStorage, terminate, score);
    }

    public static class Builder extends AnalyzerUnit.Builder {
        public Builder(boolean terminate, float score) {
            super(terminate, score);
        }

        @Override
        protected AnalyzerUnit newAnalyzerUnit(Tag.Storage tagStorage) {
            tagStorage.newGrammeme(Lists.newArrayList("NUMB", "", "ЧИСЛО", "число"));
            tagStorage.newGrammeme(Lists.newArrayList("intg", "", "цел", "целое"));
            tagStorage.newGrammeme(Lists.newArrayList("real", "", "вещ", "вещественное"));
            tagStorage.newTag("NUMB,intg");
            tagStorage.newTag("NUMB,real");
            return new NumberUnit(tagStorage, terminate, score);
        }
    }

    @Override
    public List<ParsedWord> parse(String word, String wordLower) {
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
}
