package company.evo.jmorphy2.nlp;

import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;


public class Rule {
    public final String leftStr;
    public final String rightStr;
    public final Set<String> left;
    public final List<NodeMatcher> right;
    public final int rightSize;
    public final float weight;

    public Rule(String left, String right, float weight) {
        this.leftStr = left;
        this.rightStr = right;
        this.left = parseLeft(left);
        this.right = parseRight(right);
        this.rightSize = this.right.size();
        this.weight = weight;
    }

    protected Set<String> parseLeft(String left) {
        return Set.of(left.trim().replaceAll("[@$!]", "").split(","));
    }

    protected List<NodeMatcher> parseRight(String right) {
        List<NodeMatcher> listBuilder = new ArrayList<>();
        for (String part : right.trim().split(" ")) {
            int flags = 0;
            if (part.startsWith("@")) {
                flags |= NodeMatcher.NO_COMMONS;
            }
            if (part.startsWith("$")) {
                flags |= NodeMatcher.NO_REDUCE;
            }
            if ((part.startsWith("'") && part.endsWith("'")) ||
                (part.startsWith("\"") && part.endsWith("\""))) {
                listBuilder.add(new NodeMatcher(
                    null, part.replaceAll("'", "").replaceAll("\"", ""), flags));
            } else {
                listBuilder.add(new NodeMatcher(Set.of(part.trim().replaceAll("[@$!]", "").split(",")), null, flags));
            }
        }
        return listBuilder;
    }

    public boolean match(List<Node> nodes) {
        int n = right.size();
        if (nodes.size() != n) {
            return false;
        }

        for (int i = 0; i < n; i++) {
            if (!right.get(i).match(nodes.get(i))) {
                return false;
            }
        }
        return true;
    }

    public Set<String> commonGrammemeValues(List<Node> nodes, Set<String> allowedValues) {
        Set<String> values = new HashSet<>();
        int i = 0;
        for (Node node : nodes) {
            if ((right.get(i).flags & NodeMatcher.NO_COMMONS) != 0) {
                i++;
                continue;
            }
            if (values.isEmpty()) {
                values.addAll(node.grammemeValues);
            } else {
                values.retainAll(node.grammemeValues);
            }
            i++;
        }
        values.retainAll(allowedValues);
        values.addAll(left);
        return values;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s [%s]", leftStr, rightStr, weight);
    }

    public static class NodeMatcher {
        public final Set<String> grammemeValues;
        public final String word;
        public final int flags;

        public final static int NO_COMMONS = 0x01;
        public final static int NO_REDUCE = 0x02;

        public NodeMatcher(Set<String> grammemeValues, String word, int flags) {
            this.grammemeValues = grammemeValues;
            this.word = word == null ? null : word.toLowerCase();
            this.flags = flags;
        }

        public boolean match(Node node) {
            if (grammemeValues != null) {
                if (!node.grammemeValues.containsAll(grammemeValues)) {
                    return false;
                }
            }
            if (word != null) {
                return word.equals(node.word.toLowerCase());
            }
            return true;
        }
    }
}
