package net.uaprom.jmorphy2;

import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;


public class ParsedWord implements Comparable {
    public static final float EPS = 1e-6f;

    public final String word;
    public final Tag tag;
    public final String normalForm;
    public final String foundWord;
    public final float score;
    public final AnalyzerUnit unit;

    public ParsedWord(String word, Tag tag, String normalForm, String foundWord, float score, AnalyzerUnit unit) {
        this.word = word;
        this.tag = tag;
        this.normalForm = normalForm;
        this.foundWord = foundWord;
        this.score = score;
        this.unit = unit;
    }

    public ParsedWord rescore(float newScore) {
        return new ParsedWord(word, tag, normalForm, foundWord, newScore, unit);
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }

        ParsedWord other = (ParsedWord) obj;
        return word.equals(other.word)
            && tag.equals(other.tag)
            && normalForm.equals(other.normalForm)
            && Math.abs(score - other.score) < EPS
            && unit == other.unit;
    }

    @Override
    public String toString() {
        return String.format("<Parsed: %s, %s, %s, %s, %.6f, %s>", word, tag, normalForm, foundWord, score, unit.getClass());
    }

    @Override
    public int compareTo(Object obj) {
        ParsedWord other = (ParsedWord) obj;
        if (score > other.score) return 1;
        if (score < other.score) return -1;
        return 0;
    }
}
