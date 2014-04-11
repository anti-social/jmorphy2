package net.uaprom.jmorphy2.nlp;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Comparator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.base.Joiner;
import com.google.common.base.Function;
import com.google.common.hash.HashCode;
import com.google.common.collect.Ordering;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;


public class Node {
    public final ImmutableSet<String> grammemeValues;
    public final String grammemeValuesStr;
    public final ImmutableList<Node> children;
    public final String word;
    public final float score;
    private final Integer cachedHashCode;

    public Node(ImmutableSet<String> grammemeValues, ImmutableList<Node> children, float score) {
        this(grammemeValues, children, null, score);
    }

    public Node(ImmutableSet<String> grammemeValues, String word, float score) {
        this(grammemeValues, null, word, score);
    }

    protected Node(ImmutableSet<String> grammemeValues, ImmutableList<Node> children, String word, float score) {
        if (grammemeValues == null) {
            throw new RuntimeException("grammemeValues must not be null");
        }
        this.grammemeValues = grammemeValues;
        this.grammemeValuesStr = Joiner.on(",").join(Ordering.natural().sortedCopy(grammemeValues));
        this.children = children;
        this.word = word;
        this.score = score;
        this.cachedHashCode = calcHashCode();
    }

    public boolean hasChildren() {
        return children != null;
    }

    public ImmutableList<Node> getChildren() {
        return children;
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

    public String getCacheKey() {
        String w = "";
        if (word != null) {
            w = String.format("'%s'", word);
        }
        return String.format("%s%s", w, grammemeValuesStr);
    }

    public static Function<Node,String> cacheKeyFunc() {
        return new Function<Node,String>() {
            @Override
            public String apply(Node node) {
                return node.getCacheKey();
            }
        };
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

    public static class Top extends Node {
        public Top(ImmutableList<Node> children, float score) {
            super(ImmutableSet.of("TOP"), children, score);
        }

        @Override
        public String toString() {
            return String.format("%s [%s]", super.toString(), score);
        }
    };
}
