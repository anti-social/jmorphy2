package company.evo.jmorphy2.nlp;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;


public class SubjectExtractor {
    private final Parser parser;
    private final boolean normalize;
    private List<Set<String>> enableExtractionValues;
    private List<Set<String>> disableExtractionValues;
    private List<Set<String>> subjValues;

    public SubjectExtractor(Parser parser, String confStr, boolean normalize) {
        this.parser = parser;
        this.normalize = normalize;
        loadConfigString(confStr);
    }

    private void loadConfigString(String confStr) {
        enableExtractionValues = new ArrayList<>();
        disableExtractionValues = new ArrayList<>();
        subjValues = new ArrayList<>();
        for (String part : confStr.trim().split(" ")) {
            if (part.startsWith("+")) {
                enableExtractionValues.add(parsePart(part.substring(1)));
            } else if (part.startsWith("-")) {
                disableExtractionValues.add(parsePart(part.substring(1)));
            } else {
                subjValues.add(parsePart(part));
            }
        }
    }

    private Set<String> parsePart(String part) {
        Set<String> parts = new HashSet<>();
        Collections.addAll(parts, part.trim().split(","));
        return parts;
    }

    public List<String> extract(String[] tokens) throws IOException {
        return extract(parser.parse(tokens));
    }

    public List<String> extract(Node.Top sent) {
        List<String> results = new ArrayList<>();
        for (Token token : extractTokens(sent)) {
            results.add(token.word);
        }
        return results;
    }

    public List<Token> extractTokens(String[] tokens) throws IOException {
        return extractTokens(parser.parse(tokens));
    }

    public List<Token> extractTokens(Node.Top sent) {
        List<Token> results = new ArrayList<>();
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
    }
}
