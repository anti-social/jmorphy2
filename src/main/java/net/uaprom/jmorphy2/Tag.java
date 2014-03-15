package net.uaprom.jmorphy2;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;


public class Tag {
    public static Set<Grammeme> knownGrammemes = new HashSet<Grammeme>();
    public static Map<String, Grammeme> grammemesMap = new HashMap<String, Grammeme>();

    public interface Grammeme {};

    public enum PartOfSpeach implements Grammeme {
        NOUN, // имя существительное
        ADJF, // имя прилагательное (полное)
        ADJS, // имя прилагательное (краткое)
        COMP, // компаратив
        VERB, // глагол (личная форма)
        INFN, // глагол (инфинитив)
        PRTF, // причастие (полное)
        PRTS, // причастие (краткое)
        GRND, // деепричастие
        NUMR, // числительное
        ADVB, // наречие
        NPRO, // местоимение-существительное
        PRED, // предикатив
        PREP, // предлог
        CONJ, // союз
        PRCL, // частица
        INTJ, // междометие
    };

    public enum Case implements Grammeme {
        nomn, // именительный падеж
        gent, // родительный падеж
        datv, // дательный падеж
        accs, // винительный падеж
        ablt, // творительный падеж
        loct, // предложный падеж
        voct, // звательный падеж
        gen1, // первый родительный падеж
        gen2, // второй родительный (частичный) падеж
        acc2, // второй винительный падеж
        loc1, // первый предложный падеж
        loc2, // второй предложный (местный) падеж
    };

    public enum Gender implements Grammeme {
        masc, // мужской род
        femn, // женский род
        neut, // средний род
    };

    static {
        fillKnownGrammemes(PartOfSpeach.values());
        fillKnownGrammemes(Case.values());
        fillKnownGrammemes(Gender.values());
    }

    public Set<Grammeme> grammemes = new HashSet<Grammeme>();
    
    public Tag(String tagString) {
        String[] grammemeStrings = tagString.replace(" ", ",").split(",");
        for (String grammemeName : grammemeStrings) {
            Grammeme grammeme = grammemesMap.get(grammemeName);
            if (grammeme != null) {
                grammemes.add(grammeme);
            }
        }
    }

    private static void fillKnownGrammemes(Grammeme[] grammemes) {
        for (Grammeme grammeme : grammemes) {
            knownGrammemes.add(grammeme);
            grammemesMap.put(grammeme.toString(), grammeme);
        }
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
        return grammemes.toString();
    }
}
