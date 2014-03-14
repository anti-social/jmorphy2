package net.uaprom.jmorphy2;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.noggit.JSONParser;

import org.apache.commons.io.input.SwappedDataInputStream;


public class Dictionary {
    // private static final Logger logger = LoggerFactory.getLogger(Pymorphy2Dictionary.class);
    
    private DAWG dawg;
    private ArrayList<DAWG.Paradigm> paradigms;
    private String[] suffixes;
    private String[] paradigmPrefixes;
    private HashMap<Character,String> replaceChars;

    // TODO: load metadata
    private static final String META_FILENAME = "meta.json";
    private static final String WORDS_FILENAME = "words.dawg";
    private static final String PARADIGMS_FILENAME = "paradigms.array";
    private static final String SUFFIXES_FILENAME = "suffixes.json";
    private static final String PARADIGM_PREFIXES_FILENAME = "paradigm-prefixes.json";

    private static final Logger logger = LoggerFactory.getLogger(Dictionary.class);

    public Dictionary(String path) throws IOException {
        // logger.info(path);
        // logger.info(path + "/" + WORDS_FILENAME);
        this(new FileInputStream(path + "/" + WORDS_FILENAME),
             new FileInputStream(path + "/" + PARADIGMS_FILENAME),
             new FileInputStream(path + "/" + SUFFIXES_FILENAME),
             new FileInputStream(path + "/" + PARADIGM_PREFIXES_FILENAME),
             null);
    }

    public Dictionary(InputStream dawgStream, InputStream paradigmsStream, InputStream suffixesStream, InputStream paradigmPrefixesStream, InputStream replacesStream) throws IOException {
        dawg = new DAWG(dawgStream);

        loadParadigms(paradigmsStream);
        loadSuffixes(suffixesStream);
        loadParadigmPrefixes(paradigmPrefixesStream);

        if (replacesStream != null) {
            loadReplaceChars(replacesStream);
        }
    }

    private void loadParadigms(InputStream stream) throws IOException {
        DataInput paradigmsStream = new SwappedDataInputStream(stream);
        short paradigmsCount = paradigmsStream.readShort();
        paradigms = new ArrayList<DAWG.Paradigm>(paradigmsCount);
        for (int paraId = 0; paraId < paradigmsCount; paraId++) {
            paradigms.add(new DAWG.Paradigm(paradigmsStream));
        }
    }

    private String[] readJsonStrings(InputStream stream) throws IOException {
        ArrayList<String> stringList = new ArrayList<String>();
        String[] stringArray;

        JSONParser parser = new JSONParser(new BufferedReader(new InputStreamReader(stream)));
        int event;
        while ((event = parser.nextEvent()) != JSONParser.EOF) {
            if (event == JSONParser.STRING) {
                stringList.add(parser.getString());
            }
        }
        
        stringArray = new String[stringList.size()];
        return stringList.toArray(stringArray);
    }

    private void loadSuffixes(InputStream stream) throws IOException {
        suffixes = readJsonStrings(stream);
    }

    private void loadParadigmPrefixes(InputStream stream) throws IOException {
        paradigmPrefixes = readJsonStrings(stream);
    }

    private void loadReplaceChars(InputStream stream) throws IOException {
        int i = 0;
        Character c = null;
        for (String letter : readJsonStrings(stream)) {
            if (i % 2 == 0) {
                if (letter.length() != 1) {
                    throw new IOException(String.format("Replaceable string must contain only one character: '%s'", letter));
                }

                c = letter.charAt(0);
            }
            else {
                if (replaceChars == null) {
                    replaceChars = new HashMap<Character,String>();
                }
                replaceChars.put(c, letter);
            }

            i++;
        }
    }

    public ArrayList<String> getNormalForms(char[] word, int offset, int count) throws IOException {
        ArrayList<String> normalForms = new ArrayList<String>();
        String w = new String(word, offset, count);

        ArrayList<DAWG.FoundParadigm> foundParadigms = dawg.similarItems(w, replaceChars);;

        HashSet<String> uniqueNormalForms = new HashSet<String>();
        for (DAWG.FoundParadigm foundParadigm : foundParadigms) {
            String nf = buildNormalForm(foundParadigm.paradigmId,
                                        foundParadigm.idx,
                                        foundParadigm.key);
            if (!uniqueNormalForms.contains(nf)) {
                normalForms.add(nf.toLowerCase());
                uniqueNormalForms.add(nf);
            }
        }
        
        return normalForms;
    }

    protected String buildNormalForm(short paradigmId, short idx, String word) {
        DAWG.Paradigm paradigm = paradigms.get(paradigmId);
        int paradigmLength = paradigm.paradigm.length / 3;
        String stem = buildStem(paradigm.paradigm, idx, word);

        int prefixId = paradigm.paradigm[paradigmLength * 2 + 0] & 0xFFFF;
        int suffixId = paradigm.paradigm[0] & 0xFFFF;

        String prefix = paradigmPrefixes[prefixId];
        String suffix = suffixes[suffixId];
        
        return prefix + stem + suffix;
    }

    protected String buildStem(short[] paradigm, short idx, String word) {
        int paradigmLength = paradigm.length / 3;
        int prefixId = paradigm[paradigmLength * 2 + idx] & 0xFFFF;
        String prefix = paradigmPrefixes[prefixId];
        int suffixId = paradigm[idx] & 0xFFFF;
        String suffix = suffixes[suffixId];

        if (!suffix.equals("")) {
            return word.substring(prefix.length(), word.length() - suffix.length());
        }
        else {
            return word.substring(prefix.length());
        }
    }
}
