package net.uaprom.jmorphy2.nlp;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;


public class SubjectExtractor {
    private final List<Set<String>> enableExtractionValues;
    private final List<Set<String>> disableExtractionValues;
    private final List<Set<String>> subjValues;
    private final boolean normalize;

    public SubjectExtractor(List<Set<String>> enableExtractionValues,
                            List<Set<String>> disableExtractionValues,
                            List<Set<String>> subjValues,
                            boolean normalize) {
        this.enableExtractionValues = enableExtractionValues;
        this.disableExtractionValues = disableExtractionValues;
        this.subjValues = subjValues;
        this.normalize = normalize;
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
