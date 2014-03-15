package net.uaprom.jmorphy2;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MorphAnalyzer {
    private Dictionary dict;

    // private static final String ENV_DICT_PATH = "PYMORPHY2_DICT_PATH";
    private static final String DICT_PATH_VAR = "dictPath";

    private static final Logger logger = LoggerFactory.getLogger(MorphAnalyzer.class);

    public MorphAnalyzer() throws IOException {
        this(null);
    }

    public MorphAnalyzer(String path) throws IOException {
        if (path == null) {
            path = System.getProperty(DICT_PATH_VAR);
        }
        dict = new Dictionary(path);
    }

    public List<Parsed> parse(String word) throws IOException {
        return dict.parse(word);
    }

    public List<String> getNormalForms(String word) throws IOException {
        List<Parsed> parseds = dict.parse(word);
        List<String> normalForms = new ArrayList<String>();
        Set<String> uniqueNormalForms = new HashSet<String>();

        for (Parsed p : parseds) {
            if (!uniqueNormalForms.contains(p.normalForm)) {
                normalForms.add(p.normalForm);
                uniqueNormalForms.add(p.normalForm);
            }
        }
        return normalForms;
    }
}
