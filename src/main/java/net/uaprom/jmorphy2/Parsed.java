package net.uaprom.jmorphy2;


public class Parsed {
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
            && score == other.score;
    }

    @Override
    public String toString() {
        return String.format("<Parsed: %s, %s, %s, %.2f>", word, tag, normalForm, score);
    }
}
