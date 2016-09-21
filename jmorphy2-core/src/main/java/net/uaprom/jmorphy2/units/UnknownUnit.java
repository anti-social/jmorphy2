package net.uaprom.jmorphy2.units;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import net.uaprom.jmorphy2.ParsedWord;
import net.uaprom.jmorphy2.Tag;


public class UnknownUnit extends AnalyzerUnit {
    private UnknownUnit(Tag.Storage tagStorage, boolean terminate, float score) {
        super(tagStorage, terminate, score);
    }

    public static class Builder extends AnalyzerUnit.Builder {
        public Builder(boolean terminate, float score) {
            super(terminate, score);
        }

        @Override
        protected AnalyzerUnit newAnalyzerUnit(Tag.Storage tagStorage) {
            tagStorage.newGrammeme(Lists.newArrayList("UNKN", "", "НЕИЗВ", "неизвестное"));
            tagStorage.newTag("UNKN");
            return new UnknownUnit(tagStorage, terminate, score);
        }
    }

    @Override
    public List<ParsedWord> parse(String word, String wordLower) {
        List<ParsedWord> parseds = new ArrayList<>();
        parseds.add(new AnalyzerParsedWord(word, tagStorage.getTag("UNKN"), word, word, score));
        return parseds;
    }
}
