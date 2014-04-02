package net.uaprom.jmorphy2;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;


public class Tag {
    public static final String PART_OF_SPEECH = "POST";
    public static final String ANIMACY = "ANim";
    public static final String GENDER = "GNdr";
    public static final String NUMBER = "NMbr";
    public static final String CASE = "CAse";
    public static final String ASPECT = "ASpc";
    public static final String TRANSITIVITY = "TRns";
    public static final String PERSON = "PErs";
    public static final String TENSE = "TEns";
    public static final String MOOD = "MOod";
    public static final String VOICE = "VOic";
    public static final String INVOLVEMENT = "INvl";
    private static final ImmutableSet<String> NON_PRODUCTIVE_GRAMMEMES =
        ImmutableSet.of("NUMR", "NPRO", "PRED", "PREP", "CONJ", "PRCL", "INTJ", "Apro");

    private final String originalTagString;
    private final Storage storage;

    public final ImmutableSet<Grammeme> grammemes;
    public final Grammeme POS;
    public final Grammeme anymacy;
    public final Grammeme aspect;
    public final Grammeme Case;
    public final Grammeme gender;
    public final Grammeme involvement;
    public final Grammeme mood;
    public final Grammeme number;
    public final Grammeme person;
    public final Grammeme tense;
    public final Grammeme transitivity;
    public final Grammeme voice;

    public Tag(String tagString, Storage storage) {
        this.originalTagString = tagString;
        this.storage = storage;

        Set<Grammeme> grammemes = new HashSet<Grammeme>();
        String[] grammemeStrings = tagString.replace(" ", ",").split(",");
        for (String grammemeValue : grammemeStrings) {
            if (grammemeValue != null) {
                grammemes.add(storage.getGrammeme(grammemeValue));
            }
        }
        this.grammemes = ImmutableSet.copyOf(grammemes);

        POS = getGrammemeFor(PART_OF_SPEECH);
        anymacy = getGrammemeFor(ANIMACY);
        aspect = getGrammemeFor(ASPECT);
        Case = getGrammemeFor(CASE);
        gender = getGrammemeFor(GENDER);
        involvement = getGrammemeFor(INVOLVEMENT);
        mood = getGrammemeFor(MOOD);
        number = getGrammemeFor(NUMBER);
        person = getGrammemeFor(PERSON);
        tense = getGrammemeFor(TENSE);
        transitivity = getGrammemeFor(TRANSITIVITY);
        voice = getGrammemeFor(VOICE);
    }

    private Grammeme getGrammemeFor(String rootValue) {
        for (Grammeme grammeme : this.grammemes) {
            Grammeme rootGrammeme = grammeme.getRoot();
            if (rootGrammeme != null && rootGrammeme.equals(rootValue)) {
                return grammeme;
            }
        }
        return null;
    }

    public Set<String> getGrammemeValues() {
        Set<String> grammemeValues = new HashSet<String>();
        for (Grammeme grammeme : grammemes) {
            grammemeValues.add(grammeme.value);
        }
        return grammemeValues;
    }

    public boolean contains(String grammemeValue) {
        return grammemes.contains(storage.getGrammeme(grammemeValue));
    }

    public boolean contains(Grammeme grammeme) {
        return grammemes.contains(grammeme);
    }

    public boolean containsAll(Collection<Grammeme> grammemes) {
        return this.grammemes.containsAll(grammemes);
    }

    public boolean containsAllValues(Collection<String> grammemeValues) {
        for (String grammemeValue : grammemeValues) {
            if (!grammemes.contains(storage.getGrammeme(grammemeValue))) {
                return false;
            }
        }
        return true;
    }

    public boolean isProductive() {
        return Sets.intersection(getGrammemeValues(), NON_PRODUCTIVE_GRAMMEMES).isEmpty();
    }

    public String getTagString() {
        return originalTagString;
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }

        Tag other = (Tag) obj;
        return grammemes.equals(other.grammemes);
    }

    @Override
    public String toString() {
        return originalTagString;
    }

    public static class Storage {
        private final Map<String,Tag> tags = new HashMap<String,Tag>();
        private final Map<String,Grammeme> grammemes = new HashMap<String,Grammeme>();

        public Tag getTag(String tagString) {
            return tags.get(tagString);
        }

        public Collection<Tag> getAllTags() {
            return tags.values();
        }

        private void addTag(Tag tag) {
            tags.put(tag.getTagString(), tag);
        }

        public Tag newTag(String tagString) {
            Tag tag = getTag(tagString);
            if (tag == null) {
                tag = new Tag(tagString, this);
                addTag(tag);
            }
            return tag;
        }

        public Grammeme getGrammeme(String grammemeValue) {
            return grammemes.get(grammemeValue);
        }

        public Collection<Grammeme> getAllGrammemes() {
            return grammemes.values();
        }

        private void addGrammeme(Grammeme grammeme) {
            grammemes.put(grammeme.value, grammeme);
        }

        public Grammeme newGrammeme(List<String> grammemeInfo) {
            Grammeme grammeme = getGrammeme(grammemeInfo.get(0));
            if (grammeme == null) {
                grammeme = new Grammeme(grammemeInfo, this);
                addGrammeme(grammeme);
            }
            return grammeme;
        }
    };
}
