package net.uaprom.jmorphy2;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;


public class Tag {
    public static Set<String> PARTS_OF_SPEECH =
        new HashSet(Arrays.asList(
                                  "NOUN", // имя существительное
                                  "ADJF", // имя прилагательное (полное)
                                  "ADJS", // имя прилагательное (краткое)
                                  "COMP", // компаратив
                                  "VERB", // глагол (личная форма)
                                  "INFN", // глагол (инфинитив)
                                  "PRTF", // причастие (полное)
                                  "PRTS", // причастие (краткое)
                                  "GRND", // деепричастие
                                  "NUMR", // числительное
                                  "ADVB", // наречие
                                  "NPRO", // местоимение-существительное
                                  "PRED", // предикатив
                                  "PREP", // предлог
                                  "CONJ", // союз
                                  "PRCL", // частица
                                  "INTJ"  // междометие
                                  ));

    public static Set<String> ANIMACY =
        new HashSet(Arrays.asList(
                                  "anim", // одушевлённое
                                  "inan" // неодушевлённое
                                  ));

    public static Set<String> GENDERS =
        new HashSet(Arrays.asList(
                                  "masc", // мужской род
                                  "femn", // женский род
                                  "neut"  // средний род
                                  ));

    public static Set<String> NUMBERS =
        new HashSet(Arrays.asList(
                                  "sing", // единственное число
                                  "plur"  // множественное число
                                  ));

    public static Set<String> CASES =
        new HashSet(Arrays.asList(
                                  "nomn", // именительный падеж
                                  "gent", // родительный падеж
                                  "datv", // дательный падеж
                                  "accs", // винительный падеж
                                  "ablt", // творительный падеж
                                  "loct", // предложный падеж
                                  "voct", // звательный падеж
                                  "gen1", // первый родительный падеж
                                  "gen2", // второй родительный (частичный) падеж
                                  "acc2", // второй винительный падеж
                                  "loc1", // первый предложный падеж
                                  "loc2"  // второй предложный (местный) падеж
                                  ));

    public static Set<String> ASPECTS =
        new HashSet(Arrays.asList(
                                  "perf", // совершенный вид
                                  "impf" // несовершенный вид
                                  ));

    public static Set<String> TRANSITIVITY =
        new HashSet(Arrays.asList(
                                  "tran", // переходный
                                  "intr" // непереходный
                                  ));

    public static Set<String> PERSONS =
        new HashSet(Arrays.asList(
                                  "1per", // 1 лицо
                                  "2per", // 2 лицо
                                  "3per" // 3 лицо
                                  ));

    public static Set<String> TENSES =
        new HashSet(Arrays.asList(
                                  "pres", // настоящее время
                                  "past", // прошедшее время
                                  "futr" // будущее время
                                  ));

    public static Set<String> MOODS =
        new HashSet(Arrays.asList(
                                  "indc", // изъявительное наклонение
                                  "impr" // повелительное наклонение
                                  ));

    public static Set<String> VOICES =
        new HashSet(Arrays.asList(
                                  "actv", // действительный залог
                                  "pssv" // страдательный залог
                                  ));

    public static Set<String> INVOLVEMENT =
        new HashSet(Arrays.asList(
                                  "incl", // говорящий включён в действие
                                  "excl" // говорящий не включён в действие
                                  ));

    public final String originalTagString;
    public Set<String> grammemes = new HashSet<String>();
    public final String POS;
    public final String anymacy;
    public final String aspect;
    public final String Case;
    public final String gender;
    public final String involvement;
    public final String mood;
    public final String number;
    public final String person;
    public final String tense;
    public final String transitivity;
    public final String voice;
    
    public Tag(String tagString) {
        originalTagString = tagString;
        String[] grammemeStrings = tagString.replace(" ", ",").split(",");
        for (String grammemeValue : grammemeStrings) {
            if (grammemeValue != null) {
                grammemes.add(grammemeValue);
            }
        }
        POS = getGrammemeFor(PARTS_OF_SPEECH);
        anymacy = getGrammemeFor(ANIMACY);
        aspect = getGrammemeFor(ASPECTS);
        Case = getGrammemeFor(CASES);
        gender = getGrammemeFor(GENDERS);
        involvement = getGrammemeFor(INVOLVEMENT);
        mood = getGrammemeFor(MOODS);
        number = getGrammemeFor(NUMBERS);
        person = getGrammemeFor(PERSONS);
        tense = getGrammemeFor(TENSES);
        transitivity = getGrammemeFor(TRANSITIVITY);
        voice = getGrammemeFor(VOICES);
    }

    private String getGrammemeFor(Set<String> grammemes) {
        for (String grammeme : this.grammemes) {
            if (grammemes.contains(grammeme)) {
                return grammeme;
            }
        }
        return null;
    }

    public boolean contains(String grammeme) {
        return grammemes.contains(grammeme);
    }

    public boolean containsAll(Collection<String> grammemes) {
        return this.grammemes.containsAll(grammemes);
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
        // return grammemes.toString();
    }
}
