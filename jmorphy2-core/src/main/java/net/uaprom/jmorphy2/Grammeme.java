package net.uaprom.jmorphy2;

import java.util.Map;
import java.util.HashMap;
import java.util.List;


public class Grammeme {
    public final String key;
    public final String value;
    public final String parentValue;
    public final String russianValue;
    public final String description;

    private final Tag.Storage storage;

    public Grammeme(List<String> grammemeInfo, Tag.Storage storage) {
        this(grammemeInfo.get(0),
             grammemeInfo.get(1),
             grammemeInfo.get(2),
             grammemeInfo.get(3),
             storage);
    }

    public Grammeme(String value, String parentValue, String russianValue, String description, Tag.Storage storage) {
        this.key = value.toLowerCase();
        this.value = value;
        this.parentValue = stringOrNull(parentValue);
        this.russianValue = russianValue;
        this.description = description;
        this.storage = storage;
    }

    private String stringOrNull(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        return s;
    }

    public Grammeme getParent() {
        return storage.getGrammeme(parentValue);
    }

    public Grammeme getRoot() {
        Grammeme grammeme = this;
        Grammeme parentGrammeme = grammeme.getParent();
        if (parentGrammeme == null) {
            return null;
        }
        while (parentGrammeme != null) {
            grammeme = parentGrammeme;
            parentGrammeme = grammeme.getParent();
        }
        return grammeme;
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
