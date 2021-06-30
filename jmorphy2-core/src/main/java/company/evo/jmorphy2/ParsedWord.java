package company.evo.jmorphy2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public abstract class ParsedWord implements Comparable<ParsedWord> {
    public static final float EPS = 1e-6f;

    public final String word;
    public final Tag tag;
    public final String normalForm;
    public final String foundWord;
    public final float score;

    public ParsedWord(String word, Tag tag, String normalForm, String foundWord, float score) {
        this.word = word;
        this.tag = tag;
        this.normalForm = normalForm;
        this.foundWord = foundWord;
        this.score = score;
    }

    public abstract ParsedWord rescore(float newScore);

    public abstract List<ParsedWord> getLexeme();

    public List<ParsedWord> inflect(Collection<Grammeme> requiredGrammemes) {
        return inflect(requiredGrammemes, Collections.emptyList());
    }

    public List<ParsedWord> inflect(Collection<Grammeme> requiredGrammemes,
            Collection<Grammeme> excludeGrammemes) {
        List<ParsedWord> paradigm = new ArrayList<>();
        for (ParsedWord p : getLexeme()) {
            if (p.tag.containsAll(requiredGrammemes) && !p.tag.containsAny(excludeGrammemes)) {
                paradigm.add(p);
            }
        }
        return paradigm;
    }

    @Override
    public String toString() {
        return String.format("<ParsedWord: \"%s\", \"%s\", \"%s\", \"%s\", %.6f>",
            word, tag, normalForm, foundWord, score);
    }

    @Override
    public int compareTo(ParsedWord other) {
        return Float.compare(score, other.score);
    }

    public Unique toUnique() {
        return new Unique(tag, normalForm);
    }

    public static class Unique {
        public final Tag tag;
        public final String normalForm;

        public Unique(Tag tag, String normalForm) {
            this.tag = tag;
            this.normalForm = normalForm;
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass()) {
                return false;
            }

            Unique other = (Unique) obj;
            return tag.equals(other.tag)
                && normalForm.equals(other.normalForm);
        }

        @Override
        public int hashCode() {
            int h = 17;
            h = h * 37 + tag.hashCode();
            h = h * 37 + normalForm.hashCode();
            return h;
        }
    }
}
