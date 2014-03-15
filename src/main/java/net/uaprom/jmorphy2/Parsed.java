package net.uaprom.jmorphy2;


class Parsed {
    public String word;
    public Tag tag;
    public String normalForm;
    public float score;

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
        return String.format("<Tag: %s, %s, %s, %.2f>", word, tag, normalForm, score);
    }
}
