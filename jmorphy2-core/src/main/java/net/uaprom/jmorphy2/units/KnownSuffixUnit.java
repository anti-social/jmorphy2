package net.uaprom.jmorphy2.units;

import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import net.uaprom.jmorphy2.ParsedWord;
import net.uaprom.jmorphy2.Tag;
import net.uaprom.jmorphy2.Dictionary;
import net.uaprom.jmorphy2.PredictionSuffixesDAWG;
import net.uaprom.jmorphy2.PredictionSuffixesDAWG.SuffixForm;


public class KnownSuffixUnit extends AnalyzerUnit {
    private final Dictionary dict;
    private Map<Character,String> charSubstitutes;
    private final int minWordLength;
    private final int maxSuffixLength;

    private KnownSuffixUnit(Tag.Storage tagStorage,
                            Dictionary dict,
                            Map<Character,String> charSubstitutes,
                            int minWordLength,
                            int maxSuffixLength,
                            boolean terminate,
                            float score) {
        super(tagStorage, terminate, score);
        this.dict = dict;
        this.charSubstitutes = charSubstitutes;
        this.minWordLength = minWordLength;
        this.maxSuffixLength = maxSuffixLength;
    }

    public static class Builder extends AnalyzerUnit.Builder {
        private static final int DEFAULT_MAX_SUFFIX_LENGTH = 5;
        private static final int DEFAULT_MIN_WORD_LENGTH = 4;

        private Dictionary.Builder dictBuilder;
        private Map<Character,String> charSubstitutes;
        private int minWordLength = DEFAULT_MIN_WORD_LENGTH;
        private int maxSuffixLength = DEFAULT_MAX_SUFFIX_LENGTH;

        public Builder(Dictionary.Builder dictBuilder,
                       boolean terminate,
                       float score) {
            super(terminate, score);
            this.dictBuilder = dictBuilder;
        }

        public Builder minWordLength(int minWordLength) {
            this.minWordLength = minWordLength;
            this.cachedUnit = null;
            return this;
        }

        public Builder maxSuffixLength(int maxSuffixLength) {
            this.maxSuffixLength = maxSuffixLength;
            this.cachedUnit = null;
            return this;
        }

        public Builder charSubstitutes(Map<Character,String> charSubstitutes) {
            this.charSubstitutes = charSubstitutes;
            this.cachedUnit = null;
            return this;
        }

        @Override
        protected AnalyzerUnit newAnalyzerUnit(Tag.Storage tagStorage) throws IOException {
            return new KnownSuffixUnit(tagStorage,
                                       dictBuilder.build(tagStorage),
                                       charSubstitutes,
                                       minWordLength,
                                       maxSuffixLength,
                                       terminate,
                                       score);
        }
    }

    @Override
    public List<ParsedWord> parse(String word, String wordLower) throws IOException {
        int wordLen = wordLower.length();
        if (wordLen < minWordLength) {
            return null;
        }

        List<ParsedWordWithPrefixId> parseds = new ArrayList<>();
        String[] paradigmPrefixes = dict.getParadigmPrefixes();
        int maxSuffixLength = Math.min(this.maxSuffixLength, wordLen);
        int[] totalCounts = new int[paradigmPrefixes.length];
        for (int prefixId = 0; prefixId < paradigmPrefixes.length; prefixId++) {
            String prefix = paradigmPrefixes[prefixId];
            if (!wordLower.startsWith(prefix)) {
                continue;
            }
            for (int i = maxSuffixLength; i >= 1; i--) {
                String wordStart = wordLower.substring(0, wordLen - i);
                String wordEnd = wordLower.substring(wordLen - i);
                PredictionSuffixesDAWG predictionSuffixes = dict.getPredictionSuffixes(prefixId);
                for (SuffixForm sf : predictionSuffixes.similarSuffixes(wordEnd, charSubstitutes)) {
                    totalCounts[prefixId] += sf.count;
                    Tag tag = dict.buildTag(sf.paradigmId, sf.idx);
                    if (!tag.isProductive()) {
                        continue;
                    }
                    String normalForm = dict.buildNormalForm(sf.paradigmId, sf.idx, wordStart + sf.word);
                    float score = this.score * sf.count;
                    parseds.add(new ParsedWordWithPrefixId(new AnalyzerParsedWord(wordStart + sf.word, tag, normalForm, sf.word, score), prefixId));
                }

                if (parseds.size() > 0) {
                    break;
                }
            }
        }
        
        List<ParsedWord> normParseds = new ArrayList<>();
        for (ParsedWordWithPrefixId p : parseds) {
            normParseds.add(p.parsedWord.rescore(p.parsedWord.score / totalCounts[p.prefixId]));
        }
        return normParseds;
    }

    class ParsedWordWithPrefixId {
        public final ParsedWord parsedWord;
        public final int prefixId;

        public ParsedWordWithPrefixId(ParsedWord parsedWord, int prefixId) {
            this.parsedWord = parsedWord;
            this.prefixId = prefixId;
        }
    }
}
