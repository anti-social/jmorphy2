package company.evo.jmorphy2.nlp;

import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;


public class SubjectExtractor {
    private final Parser parser;
    private final boolean normalize;
    private List<Set<String>> enableExtractionValues;
    private List<Set<String>> disableExtractionValues;
    private List<Set<String>> subjValues;

    private static final Splitter partsSplitter = Splitter.on(" ").trimResults().omitEmptyStrings();
    private static final Splitter valuesSplitter = Splitter.on(",").trimResults().omitEmptyStrings();

    public SubjectExtractor(Parser parser, String confStr, boolean normalize) {
        this.parser = parser;
        this.normalize = normalize;
        loadConfigString(confStr);
    }

    private void loadConfigString(String confStr) {
        enableExtractionValues = new ArrayList<Set<String>>();
        disableExtractionValues = new ArrayList<Set<String>>();
        subjValues = new ArrayList<Set<String>>();
        for (String part : partsSplitter.split(confStr)) {
            if (part.startsWith("+")) {
                enableExtractionValues.add(parsePart(part.substring(1, part.length())));
            } else if (part.startsWith("-")) {
                disableExtractionValues.add(parsePart(part.substring(1, part.length())));
            } else {
                subjValues.add(parsePart(part));
            }
        }
    }

    private Set<String> parsePart(String part) {
        return ImmutableSet.copyOf(valuesSplitter.split(part));
    }

    public List<String> extract(String[] tokens) throws IOException {
        return extract(parser.parse(tokens));
    }

    public List<String> extract(Node.Top sent) {
        List<String> results = new ArrayList<String>();
        for (Token token : extractTokens(sent)) {
            results.add(token.word);
        }
        return results;
    }

    public List<Token> extractTokens(String[] tokens) throws IOException {
        return extractTokens(parser.parse(tokens));
    }

    public List<Token> extractTokens(Node.Top sent) {
        List<Token> results = new ArrayList<Token>();
        fetchTokens(results, sent, 0, false, false);
        return results;
    }

    private int fetchTokens(List<Token> results, Node node, int index, boolean enabled, boolean disabled) {
        if (match(enableExtractionValues, node.grammemeValues)) {
            enabled = true;
        }
        if (match(disableExtractionValues, node.grammemeValues)) {
            disabled = true;
        }

        if (node.isLeaf()) {
            if (enabled && !disabled && match(subjValues, node.grammemeValues)) {
                if (normalize && node.parsed != null) {
                    results.add(new Token(node.parsed.normalForm, index));
                } else {
                    results.add(new Token(node.word, index));
                }
            }
            return 1;
        }
        
        int ixInc = 0;
        for (Node child : node.getChildren()) {
            ixInc += fetchTokens(results, child, index + ixInc, enabled, disabled);
        }
        return ixInc;
    }

    private boolean match(List<Set<String>> matchValues, Set<String> values) {
        for (Set<String> testValues : matchValues) {
            if (values.containsAll(testValues)) {
                return true;
            }
        }
        return false;
    }

    public static class Token {
        public final String word;
        public final int index;

        public Token(String word, int index) {
            this.word = word;
            this.index = index;
        }

        @Override
        public String toString() {
            return String.format("%s_%s", word, index);
        }
    };
}
