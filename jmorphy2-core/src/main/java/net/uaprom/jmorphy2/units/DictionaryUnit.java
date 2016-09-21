package net.uaprom.jmorphy2.units;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.uaprom.jmorphy2.Dictionary;
import net.uaprom.jmorphy2.ParsedWord;
import net.uaprom.jmorphy2.Tag;
import net.uaprom.jmorphy2.WordsDAWG;


public class DictionaryUnit extends AnalyzerUnit {
    private final Dictionary dict;
    private final Map<Character,String> charSubstitutes;

    public DictionaryUnit(Tag.Storage tagStorage,
                          Dictionary dict,
                          Map<Character,String> charSubstitutes,
                          boolean terminate,
                          float score) {
        super(tagStorage, terminate, score);
        this.dict = dict;
        this.charSubstitutes = charSubstitutes;
    }

    public static class Builder extends AnalyzerUnit.Builder {
        private Dictionary.Builder dictBuilder;
        private Map<Character,String> charSubstitutes;

        public Builder(Dictionary.Builder dictBuilder,
                       boolean terminate,
                       float score) {
            super(terminate, score);
            this.dictBuilder = dictBuilder;
        }

        public Builder charSubstitutes(Map<Character,String> charSubstitutes) {
            this.charSubstitutes = charSubstitutes;
            this.cachedUnit = null;
            return this;
        }

        @Override
        protected AnalyzerUnit newAnalyzerUnit(Tag.Storage tagStorage) throws IOException {
            Dictionary dict = dictBuilder.build(tagStorage);
            return new DictionaryUnit(tagStorage, dict, charSubstitutes, terminate, score);
        }
    }

    public Dictionary getDict() {
        return dict;
    }

    @Override
    public List<ParsedWord> parse(String word, String wordLower) throws IOException {
        List<ParsedWord> parseds = new ArrayList<>();
        for (Dictionary.Parsed dictParsed : dict.parse(wordLower, charSubstitutes)) {
            parseds.add(new DictionaryParsedWord(wordLower, dictParsed.tag, dictParsed.normalForm, wordLower, score, dictParsed));
        }
        return parseds;
    }

    class DictionaryParsedWord extends AnalyzerParsedWord {
        public final Dictionary.Parsed dictParsed;

        public DictionaryParsedWord(String word,
                                    Tag tag,
                                    String normalForm,
                                    String foundWord,
                                    float score,
                                    Dictionary.Parsed dictParsed) {
            super(word, tag, normalForm, foundWord, score);
            this.dictParsed = dictParsed;
        }

        @Override
        public ParsedWord rescore(float newScore) {
            return new DictionaryParsedWord(word, tag, normalForm, foundWord, newScore, dictParsed);
        }

        @Override
        public List<ParsedWord> getLexeme() {
            List<ParsedWord> lexeme = new ArrayList<>();
            for (Dictionary.Parsed p : dictParsed.iterLexeme()) {
                lexeme.add(new DictionaryParsedWord(p.word, p.tag, p.normalForm, p.word, score, p));
            }
            return lexeme;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj)
                && dictParsed.equals(((DictionaryParsedWord) obj).dictParsed);
        }
    }
}
