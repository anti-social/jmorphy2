package net.uaprom.jmorphy2.units;

import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import net.uaprom.jmorphy2.ParsedWord;
import net.uaprom.jmorphy2.Tag;
import net.uaprom.jmorphy2.Dictionary;
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
        List<ParsedWord> parseds = new ArrayList<>();
        System.out.println(String.format(" > word: %s", wordLower));
        int wordLen = wordLower.length();
        if (wordLen < minWordLength) {
            return parseds;
        }
        int maxSuffixLength = Math.min(this.maxSuffixLength, wordLen);
        int totalCount = 0;
        for (int i = maxSuffixLength; i >= 1; i--) {
            String prefix = wordLower.substring(0, wordLen - i);
            String suffix = wordLower.substring(wordLen - i, wordLen);
            System.out.println(String.format(" > prefix: %s, suffix: %s", prefix, suffix));
            for (SuffixForm sf : dict.predictionSuffixes.similarSuffixes(suffix, charSubstitutes)) {
                System.out.println(String.format(" %s: %s, %s, %s", sf.word, sf.count, sf.paradigmId, sf.idx));
                totalCount += sf.count;
                Tag tag = dict.buildTag(sf.paradigmId, sf.idx);
                if (!tag.isProductive()) {
                    continue;
                }
                String normalForm = prefix + dict.buildNormalForm(sf.paradigmId, sf.idx, sf.word);
                float score = this.score * sf.count;
                parseds.add(new AnalyzerParsedWord(prefix + sf.word, tag, normalForm, sf.word, score));
            }

            if (parseds.size() > 0) {
                break;
            }
        }

        List<ParsedWord> normParseds = new ArrayList<>();
        for (ParsedWord p : parseds) {
            normParseds.add(p.rescore(p.score / totalCount));
        }
        return normParseds;
    }
}
