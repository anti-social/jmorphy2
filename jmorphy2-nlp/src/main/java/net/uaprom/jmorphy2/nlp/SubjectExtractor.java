package net.uaprom.jmorphy2.nlp;

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
        for (Node subjNode : extract(sent, false, false)) {
            if (normalize && subjNode.parsed != null) {
                results.add(subjNode.parsed.normalForm);
            } else {
                results.add(subjNode.word);
            }
        }
        return results;
    }

    private List<Node> extract(Node node, boolean enabled, boolean disabled) {
        if (match(enableExtractionValues, node.grammemeValues)) {
            enabled = true;
        }
        if (match(disableExtractionValues, node.grammemeValues)) {
            disabled = true;
        }

        List<Node> subjNodes = new ArrayList<Node>();
        for (Node child : node.getChildren()) {
            if (child.isLeaf()) {
                if (enabled && !disabled && match(subjValues, child.grammemeValues)) {
                    subjNodes.add(child);
                }
            } else {
                subjNodes.addAll(extract(child, enabled, disabled));
            }
        }
        return subjNodes;
    }

    private boolean match(List<Set<String>> matchValues, Set<String> values) {
        for (Set<String> testValues : matchValues) {
            if (values.containsAll(testValues)) {
                return true;
            }
        }
        return false;
    }
}
