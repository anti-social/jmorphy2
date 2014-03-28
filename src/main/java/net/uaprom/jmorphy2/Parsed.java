package net.uaprom.jmorphy2;

import java.lang.Math;


public class Parsed implements Comparable {
    public static final float EPS = 1e-6f;

    public final String word;
    public final Tag tag;
    public final String normalForm;
    public final float score;

    public Parsed(String word, Tag tag, String normalForm, float score) {
        this.word = word;
        this.tag = tag;
        this.normalForm = normalForm;
        this.score = score;
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }

        Parsed other = (Parsed) obj;
        return word.equals(other.word)
            && tag.equals(other.tag)
            && normalForm.equals(other.normalForm)
            && Math.abs(score - other.score) < EPS;
    }

    @Override
    public String toString() {
        return String.format("<Parsed: %s, %s, %s, %.6f>", word, tag, normalForm, score);
    }

    @Override
    public int compareTo(Object obj) {
        Parsed other = (Parsed) obj;
        if (score > other.score) return 1;
        if (score < other.score) return -1;
        return 0;
    }
}
