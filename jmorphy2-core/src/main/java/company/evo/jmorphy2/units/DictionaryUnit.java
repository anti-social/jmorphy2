package company.evo.jmorphy2.units;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import company.evo.jmorphy2.Dictionary;
import company.evo.jmorphy2.ParsedWord;
import company.evo.jmorphy2.Tag;
import company.evo.jmorphy2.WordsDAWG;


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
    public List<ParsedWord> parse(String word, String wordLower) {
        List<ParsedWord> parseds = new ArrayList<>();
        for (WordsDAWG.WordForm wf : dict.getWords().similarWords(wordLower, charSubstitutes)) {
            String normalForm = dict.buildNormalForm(wf.paradigmId, wf.idx, wf.word);
            Tag tag = dict.buildTag(wf.paradigmId, wf.idx);
            parseds.add(new DictionaryParsedWord(wordLower, tag, normalForm, wf.word, wf, score));
        }
        return parseds;
    }

    class DictionaryParsedWord extends AnalyzerParsedWord {
        private final WordsDAWG.WordForm wordForm;

        public DictionaryParsedWord(String word,
                                    Tag tag,
                                    String normalForm,
                                    String foundWord,
                                    WordsDAWG.WordForm wordForm,
                                    float score) {
            super(word, tag, normalForm, foundWord, score);
            this.wordForm = wordForm;
        }

        @Override
        public ParsedWord rescore(float newScore) {
            return new DictionaryParsedWord(word, tag, normalForm, foundWord, wordForm, newScore);
        }

        @Override
        public List<ParsedWord> getLexeme() {
            List<ParsedWord> lexeme = new ArrayList<>();
            Dictionary.Paradigm paradigm = dict.getParadigm(wordForm.paradigmId);
            int paradigmSize = paradigm.size();
            String stem = dict.buildStem(wordForm.paradigmId, wordForm.idx, wordForm.word);
            String normalForm = dict.buildNormalForm(wordForm.paradigmId, wordForm.idx, wordForm.word);
            for (short idx = 0; idx < paradigmSize; idx++) {
                String prefix = dict.getParadigmPrefixes()[paradigm.getStemPrefixId(idx)];
                String suffix = dict.getSuffix(wordForm.paradigmId, idx);
                String word = prefix + stem + suffix;
                Tag tag = dict.buildTag(wordForm.paradigmId, idx);
                WordsDAWG.WordForm wf = new WordsDAWG.WordForm(word, wordForm.paradigmId, idx);
                lexeme.add(new DictionaryParsedWord(word, tag, normalForm, wf.word, wf, score));
            }
            return lexeme;
        }
    }
}
