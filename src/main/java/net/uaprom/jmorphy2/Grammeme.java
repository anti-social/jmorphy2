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

    protected Grammeme(String value, String parentValue, String russianValue, String description, Dictionary dict) {
        this.value = value;
        this.parentValue = parentValue;
        this.russianValue = russianValue;
        this.description = description;
        this.dict = dict;
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
}
