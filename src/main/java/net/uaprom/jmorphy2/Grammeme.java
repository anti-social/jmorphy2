package net.uaprom.jmorphy2;

import java.util.Map;
import java.util.HashMap;
import java.util.List;


public class Grammeme {
    public final String value;
    public final String parentValue;
    public final String russianValue;
    public final String description;

    private final Dictionary dict;

    public Grammeme(List<String> grammemeInfo, Dictionary dict) {
        this(grammemeInfo.get(0),
             grammemeInfo.get(1),
             grammemeInfo.get(2),
             grammemeInfo.get(3),
             dict);
    }

    public Grammeme(String value, String parentValue, String russianValue, String description, Dictionary dict) {
        this.value = value;
        this.parentValue = stringOrNull(parentValue);
        this.russianValue = russianValue;
        this.description = description;
        this.dict = dict;
    }

    private String stringOrNull(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        return s;
    }

    public Grammeme getParent() {
        return dict.getGrammeme(parentValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof String) {
            return value.equals((String) obj);
        }
        
        if (getClass() != obj.getClass()) {
            return false;
        }

        return value.equals(((Grammeme) obj).value);
    }

    @Override
    public String toString() {
        return value;
    }

    public String info() {
        return String.format("<%s, %s, %s, %s>", value, parentValue, russianValue, description);
    }
}
