package net.uaprom.jmorphy2.nlp;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import net.uaprom.jmorphy2.Parsed;


public class Node {
    public final ImmutableSet<String> grammemeValues;
    public final String grammemeValuesStr;
    public final ImmutableList<Node> children;
    public final Parsed parsed;
    public final String word;
    public final float score;
    public final int maxDepth;

    public final long uniqueHash;

    public Node(ImmutableSet<String> grammemeValues, ImmutableList<Node> children, float score) {
        this(grammemeValues, children, null, null, score);
    }

    public Node(ImmutableSet<String> grammemeValues, String word, float score) {
        this(grammemeValues, null, null, word, score);
    }

    public Node(ImmutableSet<String> grammemeValues, Parsed parsed, float score) {
        this(grammemeValues, null, parsed, parsed.word, score);
    }

    protected Node(ImmutableSet<String> grammemeValues, ImmutableList<Node> children, Parsed parsed, String word, float score) {
        if (grammemeValues == null) {
            throw new RuntimeException("grammemeValues must not be null");
        }
        this.grammemeValues = grammemeValues;
        this.grammemeValuesStr = Joiner.on(",").join(Ordering.natural().sortedCopy(grammemeValues));
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

    public ImmutableList<Node> getChildren() {
        if (hasChildren()) {
            return children;
        }
        return ImmutableList.of();
    }

    public int getChildrenSize() {
        return children == null ? 0 : children.size();
    }

    public String getWord() {
        return word;
    }

    public boolean match(ImmutableSet<String> grammemeValues, String word) {
        if (grammemeValues != null) {
            if (!this.grammemeValues.containsAll(grammemeValues)) {
                return false;
            }
        }
        if (word != null) {
            if (!word.equals(this.word)) {
                return false;
            }
        }
        return true;
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
        return new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                return Float.compare(n2.score, n1.score);
            }
        };
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
        return String.format("(%s %s)",
                             grammemeValuesStr,
                             hasChildren() ? Joiner.on(" ").join(children) : word);
    }

    public String prettyToString() {
        return prettyToString(false);
    }

    public String prettyToString(boolean withScore) {
        return prettyToString(withScore, 0);
    }

    protected String prettyToString(boolean withScore, int level) {
        String pad = level == 0 ? "" : String.format("\n%s", Strings.repeat(" ", level * 4));
        List<String> childrenStrings = new ArrayList<String>();
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
                             hasChildren() ? Joiner.on(" ").join(childrenStrings): word,
                             addInfo);
    }

    public static class Top extends Node {
        public Top(ImmutableList<Node> children, float score) {
            super(ImmutableSet.of("TOP"), children, score);
        }
    };
}
