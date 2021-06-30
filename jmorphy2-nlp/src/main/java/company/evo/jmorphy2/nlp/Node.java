package company.evo.jmorphy2.nlp;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

import company.evo.jmorphy2.ParsedWord;


public class Node {
    public final Set<String> grammemeValues;
    public final String grammemeValuesStr;
    public final List<Node> children;
    public final ParsedWord parsed;
    public final String word;
    public final float score;
    public final int maxDepth;

    public final long uniqueHash;

    public Node(Set<String> grammemeValues, List<Node> children, float score) {
        this(grammemeValues, children, null, null, score);
    }

    public Node(Set<String> grammemeValues, String word, float score) {
        this(grammemeValues, null, null, word, score);
    }

    public Node(Set<String> grammemeValues, ParsedWord parsed, float score) {
        this(grammemeValues, null, parsed, parsed.word, score);
    }

    protected Node(Set<String> grammemeValues, List<Node> children, ParsedWord parsed, String word, float score) {
        if (grammemeValues == null) {
            throw new RuntimeException("grammemeValues must not be null");
        }
        this.grammemeValues = grammemeValues;
        List<String> listGrammemeValues = new ArrayList<>(grammemeValues.size());
        listGrammemeValues.addAll(grammemeValues);
        Collections.sort(listGrammemeValues);
        this.grammemeValuesStr = String.join(",", listGrammemeValues);
        this.children = children;
        this.parsed = parsed;
        this.word = word;
        this.score = score;

        this.maxDepth = maxDepthFor(getChildren()) + 1;
        this.uniqueHash = calcUniqueHash();
    }

    public boolean hasChildren() {
        return children != null;
    }

    public boolean isLeaf() {
        return children == null;
    }

    public List<Node> getChildren() {
        if (hasChildren()) {
            return children;
        }
        return List.of();
    }

    public int getChildrenSize() {
        return children == null ? 0 : children.size();
    }

    public String getWord() {
        return word;
    }

    public static float sumScoreFor(List<Node> nodes) {
        float score = 0.0f;
        for (Node n : nodes) {
            score += n.score;
        }
        return score;
    }

    public static int maxDepthFor(List<Node> nodes) {
        int maxDepth = 0;
        for (Node n : nodes) {
            if (n.maxDepth > maxDepth) {
                maxDepth = n.maxDepth;
            }
        }
        return maxDepth;
    }

    public static Comparator<Node> scoreComparator() {
        return (n1, n2) -> Float.compare(n2.score, n1.score);
    }

    private long calcUniqueHash() {
        long h = grammemeValues.hashCode();
        if (children != null) {
            for (Node child : children) {
                h = h * 524287 + child.uniqueHash;
            }
        }
        if (word != null) {
            h = h * 2147483647 + word.hashCode();
        }
        return h;
    }

    @Override
    public String toString() {
        List<String> childrenList = new ArrayList<>();
        if (children != null) {
            for (Node child : children) {
                childrenList.add(child.prettyToString(false, 1));
            }
        }
        return String.format(
            "(%s %s)",
            grammemeValuesStr,
            hasChildren() ? String.join(" ", childrenList) : word
         ).replaceAll("\\n", "").replaceAll("( )+", " ").trim();

    }

    public String prettyToString() {
        return prettyToString(false);
    }

    public String prettyToString(boolean withScore) {
        return prettyToString(withScore, 0);
    }

    protected String prettyToString(boolean withScore, int level) {
        String pad = level == 0 ? "" : String.format("\n%s", " ".repeat(level * 4));
        List<String> childrenStrings = new ArrayList<>();
        for (Node child : getChildren()) {
            childrenStrings.add(child.prettyToString(withScore, level + 1));
        }

        String addInfo = "";
        if (withScore) {
            addInfo = String.format(" [%s]", score);
        }
        return String.format("%s(%s %s)%s",
                             pad,
                             grammemeValuesStr,
                             hasChildren() ? String.join(" ", childrenStrings): word,
                             addInfo);
    }

    public static class Top extends Node {
        public Top(List<Node> children, float score) {
            super(Set.of("TOP"), children, score);
        }
    }
}
