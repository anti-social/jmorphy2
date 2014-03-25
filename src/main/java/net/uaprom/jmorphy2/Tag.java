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

    private final String originalTagString;
    private final Dictionary dict;

    public final Set<Grammeme> grammemes;
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

    public Tag(String tagString) {
        this(tagString, null);
    }
    
    public Tag(String tagString, Dictionary dict) {
        this.originalTagString = tagString;
        this.dict = dict;

        Set<Grammeme> grammemes = new HashSet<Grammeme>();
        String[] grammemeStrings = tagString.replace(" ", ",").split(",");
        for (String grammemeValue : grammemeStrings) {
            if (grammemeValue != null) {
                grammemes.add(dict.getGrammeme(grammemeValue));
            }
        }
        this.grammemes = Collections.unmodifiableSet(grammemes);

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
        return grammemes.contains(dict.getGrammeme(grammemeValue));
    }

    public boolean contains(Grammeme grammeme) {
        return grammemes.contains(grammeme);
    }

    public boolean containsAll(Collection<Grammeme> grammemes) {
        return this.grammemes.containsAll(grammemes);
    }

    public boolean containsAllValues(Collection<String> grammemeValues) {
        for (String grammemeValue : grammemeValues) {
            if (!grammemes.contains(dict.getGrammeme(grammemeValue))) {
                return false;
            }
        }
        return true;
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
        return "\"" + originalTagString + "\"";
    }
}
