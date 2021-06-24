package company.evo.jmorphy2.lucene;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.JSONUtils;
import company.evo.jmorphy2.nlp.Ruleset;
import company.evo.jmorphy2.nlp.Tagger;
import company.evo.jmorphy2.nlp.SimpleTagger;
import company.evo.jmorphy2.nlp.Parser;
import company.evo.jmorphy2.nlp.SimpleParser;
import company.evo.jmorphy2.nlp.SubjectExtractor;


public class Jmorphy2SubjectFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    public static final String DICT_PATH_ATTR = "dict";
    public static final String REPLACES_PATH_ATTR = "replaces";
    public static final String CACHE_SIZE_ATTR = "cacheSize";
    public static final String TAGGER_RULES_PATH_ATTR = "taggerRules";
    public static final String TAGGER_THRESHOLD_ATTR = "taggerThreshold";
    public static final String PARSER_RULES_PATH_ATTR = "parserRules";
    public static final String PARSER_THRESHOLD_ATTR = "parserThreshold";
    public static final String EXTRACT_ATTR = "extract";
    public static final String MAX_SENTENCE_LENGTH_ATTR = "maxSentenceLength";

    public static final String DEFAULT_DICT_PATH = "pymorphy2_dicts";
    public static final int DEFAULT_MAX_SENTENCE_LENGTH = 10;

    private SubjectExtractor subjExtractor;
    private final String dictPath;
    private final String replacesPath;
    private final int cacheSize;
    private final String taggerRulesPath;
    private final int taggerThreshold;
    private final String parserRulesPath;
    private final int parserThreshold;
    private final String extract;
    private final int maxSentenceLength;

    public Jmorphy2SubjectFilterFactory(Map<String,String> args) {
        super(args);

        String dictPath = args.get(DICT_PATH_ATTR);
        if (dictPath == null) {
            dictPath = DEFAULT_DICT_PATH;
        }

        // morph analyzer
        this.dictPath = dictPath;
        this.replacesPath = args.get(REPLACES_PATH_ATTR);
        this.cacheSize = getInt(args, CACHE_SIZE_ATTR, Jmorphy2StemFilterFactory.DEFAULT_CACHE_SIZE);
        // tagger
        this.taggerRulesPath = args.get(TAGGER_RULES_PATH_ATTR);
        this.taggerThreshold = getInt(args, TAGGER_THRESHOLD_ATTR, SimpleTagger.DEFAULT_THRESHOLD);
        // parser
        this.parserRulesPath = args.get(PARSER_RULES_PATH_ATTR);
        this.parserThreshold = getInt(args, TAGGER_THRESHOLD_ATTR, SimpleParser.DEFAULT_THRESHOLD);
        // subject extractor
        this.extract = args.get(EXTRACT_ATTR);
        this.maxSentenceLength = getInt(args, MAX_SENTENCE_LENGTH_ATTR, DEFAULT_MAX_SENTENCE_LENGTH);
    }

    public void inform(ResourceLoader loader) throws IOException {
        Map<Character,String> replaceChars = null;
        if (replacesPath != null) {
            replaceChars = parseReplaces(loader.openResource(replacesPath));
        }

        MorphAnalyzer morph = new MorphAnalyzer.Builder()
            .fileLoader(new LuceneFileLoader(loader, dictPath))
            .charSubstitutes(replaceChars)
            .cacheSize(cacheSize)
            .build();
        Tagger tagger = new SimpleTagger(morph, new Ruleset(loader.openResource(taggerRulesPath)), taggerThreshold);
        Parser parser = new SimpleParser(morph, tagger, new Ruleset(loader.openResource(parserRulesPath)), parserThreshold);
        subjExtractor = new SubjectExtractor(parser, extract, true);
    }

    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2SubjectFilter(tokenStream, subjExtractor, maxSentenceLength);
    }

    @SuppressWarnings("unchecked")
    private Map<Character,String> parseReplaces(InputStream stream) throws IOException {
        Map<Character,String> replaceChars = new HashMap<Character,String>();
        for (Map.Entry<String,String> entry : ((Map<String,String>) JSONUtils.parseJSON(stream)).entrySet()) {
            String c = entry.getKey();
            if (c.length() != 1) {
                throw new IOException(String.format("Replaceable string must contain only one character: '%s'", c));
            }

            replaceChars.put(c.charAt(0), entry.getValue());
        }
        return replaceChars;
    }
}
