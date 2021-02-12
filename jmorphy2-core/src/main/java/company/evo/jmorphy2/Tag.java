package company.evo.jmorphy2;

import java.util.*;


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
    private static final Set<String> NON_PRODUCTIVE_GRAMMEMES =
        Set.of("NUMR", "NPRO", "PRED", "PREP", "CONJ", "PRCL", "INTJ", "Apro");

    private final String originalTagString;
    private final String normalizedTagString;
    private final Storage storage;

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

    public Tag(String tagString, Storage storage) {
        this.originalTagString = tagString;
        this.storage = storage;

        Set<Grammeme> grammemes = new HashSet<>();
        String[] grammemeStrings = Storage.splitTagString(tagString);
        List<String> normalizedGrammemeValues = new ArrayList<>(grammemeStrings.length);
        for (String grammemeValue : grammemeStrings) {
            Grammeme grammeme = storage.getGrammeme(grammemeValue);
            if (grammeme == null) {
                continue;
            }
            grammemes.add(grammeme);
            normalizedGrammemeValues.add(grammeme.key);
        }
        this.grammemes = Set.copyOf(grammemes);

        Collections.sort(normalizedGrammemeValues);
        this.normalizedTagString = String.join(" ", normalizedGrammemeValues);

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
        Grammeme rootGrammeme = storage.getGrammeme(rootValue);
        if (rootGrammeme == null) {
            return null;
        }
        for (Grammeme grammeme : this.grammemes) {
            if (rootGrammeme.equals(grammeme.getRoot())) {
                return grammeme;
            }
        }
        return null;
    }

    public Set<String> getGrammemeValues() {
        Set<String> grammemeValues = new HashSet<>();
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

    public boolean containsAny(Collection<Grammeme> grammemes) {
        for (Grammeme grammeme : grammemes) {
            if (contains(grammeme)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAnyValues(Collection<String> grammemeValues) {
        for (String grammemeValue : grammemeValues) {
            if (grammemes.contains(storage.getGrammeme(grammemeValue))) {
                return true;
            }
        }
        return false;
    }

    public boolean isProductive() {
        Set<String> isProd = getGrammemeValues();
        isProd.retainAll(NON_PRODUCTIVE_GRAMMEMES);
        return isProd.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tag) {
            Tag other = (Tag) obj;
            return grammemes.equals(other.grammemes) &&
                    storage == other.storage;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(grammemes);
    }

    @Override
    public String toString() {
        return originalTagString;
    }

    // TODO: make as API
    static public class Storage {
        private final Map<String,Tag> tags = new HashMap<>();
        private final Map<String,Grammeme> grammemes = new HashMap<>();

        static String normalizeGrammemeValue(String grammemeValue) {
            return grammemeValue.toLowerCase();
        }

        private static String[] splitTagString(String tagString) {
            return tagString.replace(" ", ",").split(",");
        }

        private static String normalizeTagString(String tagString) {
            String[] grammemeStrings = splitTagString(tagString);
            List<String> normalizedGrammemeValues = new ArrayList<>(grammemeStrings.length);
            for (String grammemeValue : grammemeStrings) {
                normalizedGrammemeValues.add(normalizeGrammemeValue(grammemeValue));
            }
            Collections.sort(normalizedGrammemeValues);
            return String.join(" ", normalizedGrammemeValues);
        }

        public Tag getTag(String tagString) {
            return tags.get(normalizeTagString(tagString));
        }

        public Collection<Tag> getAllTags() {
            return tags.values();
        }

        private void addTag(Tag tag) {
            tags.put(tag.normalizedTagString, tag);
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
            if (grammemeValue == null) {
                return null;
            }
            return grammemes.get(normalizeGrammemeValue(grammemeValue));
        }

        public Collection<Grammeme> getAllGrammemes() {
            return grammemes.values();
        }

        private void addGrammeme(Grammeme grammeme) {
            grammemes.put(grammeme.key, grammeme);
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
