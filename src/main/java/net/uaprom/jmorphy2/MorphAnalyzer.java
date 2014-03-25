package net.uaprom.jmorphy2;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MorphAnalyzer {
    private Dictionary dict;

    // private static final String ENV_DICT_PATH = "PYMORPHY2_DICT_PATH";
    private static final String DICT_PATH_VAR = "dictPath";

    protected int MAX_PREFIX_LENGTH = 6;
    protected int MIN_REMINDER = 3;

    private static final Logger logger = LoggerFactory.getLogger(MorphAnalyzer.class);

    public MorphAnalyzer() throws IOException {
        this(null, null);
    }

    public MorphAnalyzer(String path) throws IOException {
        this(path, null);
    }

    public MorphAnalyzer(Map<Character,String> replaceChars) throws IOException {
        this(null, replaceChars);
    }

    public MorphAnalyzer(String path, Map<Character,String> replaceChars) throws IOException {
        if (path == null) {
            path = System.getProperty(DICT_PATH_VAR);
        }
        dict = new Dictionary(path, replaceChars);
    }

    public Tag getTag(String tagString) {
        return dict.getTag(tagString);
    }

    public Grammeme getGrammeme(String value) {
        return dict.getGrammeme(value);
    }

    public Collection<Grammeme> getAllGrammemes() {
        return dict.getAllGrammemes();
    }

    public List<Parsed> parse(String word) throws IOException {
        List<Parsed> parseds = dict.parse(word);
        int wordLength = word.length();
        int i = 1;
        while (parseds.isEmpty()) {
            if (i > MAX_PREFIX_LENGTH || wordLength - i < MIN_REMINDER) {
                break;
            }
            parseds = dict.parse(word.substring(i));
            i++;
        }
        if (i > 1) {
            String prefix = word.substring(0, i - 1);
            List<Parsed> parsedsWithPrefix = new ArrayList<Parsed>();
            for (Parsed parsed : parseds) {
                parsedsWithPrefix.add(new Parsed(prefix + parsed.word,
                                                 parsed.tag,
                                                 prefix + parsed.normalForm,
                                                 parsed.score));
            }
            parseds = parsedsWithPrefix;
        }
        return parseds;
    }

    public List<String> getNormalForms(String word) throws IOException {
        List<Parsed> parseds = parse(word);
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
