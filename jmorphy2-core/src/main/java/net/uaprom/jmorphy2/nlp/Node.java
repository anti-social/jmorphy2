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

    private final int cachedHashCode;

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
        this.maxDepth = calcMaxDepth();
        this.score = score / this.maxDepth;
        this.cachedHashCode = calcHashCode();
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

    public static float calcScore(List<Node> nodes) {
        float score = 0.0f;
        for (Node n : nodes) {
            score += n.score;
        }
        return score;
    }

    public static Comparator<Node> scoreComparator() {
        return new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                return Float.compare(n2.score, n1.score);
            }
        };
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    private int calcHashCode() {
        int h = grammemeValues.hashCode();
        if (children != null) {
            h = h * 37 + children.hashCode();
        }
        if (word != null) {
            h = h * 37 + word.hashCode();
        }
        return h;
    }

    private int calcMaxDepth() {
        int childMaxDepth = 0;
        if (children != null) {
            for (Node child : children) {
                if (child.maxDepth > childMaxDepth) {
                    childMaxDepth = child.maxDepth;
                }
            }
        }
        return childMaxDepth + 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }

        Node other = (Node) obj;
        return grammemeValues.equals(other.grammemeValues)
            && (children == null ? other.children == null : children.equals(other.children))
            && (word == null ? other.word == null : word.equals(other.word));
    }

    @Override
    public String toString() {
        return String.format("(%s %s)",
                             grammemeValuesStr,
                             hasChildren() ? Joiner.on(" ").join(children) : word);
    }

    public String prettyToString() {
        return prettyToString(0);
    }

    protected String prettyToString(int level) {
        String pad = level == 0 ? "" : String.format("\n%s", Strings.repeat(" ", level * 4));
        List<String> childrenStrings = new ArrayList<String>();
        for (Node child : getChildren()) {
            childrenStrings.add(child.prettyToString(level + 1));
        }
        return String.format("%s(%s %s)",
                             pad,
                             grammemeValuesStr,
                             hasChildren() ? Joiner.on(" ").join(childrenStrings): word);
    }

    public static class Top extends Node {
        public Top(ImmutableList<Node> children, float score) {
            super(ImmutableSet.of("TOP"), children, score);
        }
    };
}
