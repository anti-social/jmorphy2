package net.uaprom.jmorphy2;

import java.io.IOException;
import java.util.ArrayList;

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
}
