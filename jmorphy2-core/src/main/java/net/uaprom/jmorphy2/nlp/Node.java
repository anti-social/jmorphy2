package net.uaprom.jmorphy2.nlp;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;


public class Node {
    protected final ImmutableList<Node> children;
    protected final ImmutableSet<String> values;

    public Node(ImmutableList<Node> children, ImmutableSet<String> values) {
        this.children = children;
        this.values = values;
    }

    public boolean isLeaf() {
        return children == null;
    }

    public ImmutableList<Node> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return String.format("(%s %s)",
                             Joiner.on(",").join(values),
                             Joiner.on(" ").join(children));
    }

    public static class Top extends Node {
        public final float score;

        public Top(ImmutableList<Node> children, float score) {
            super(children, ImmutableSet.of("TOP"));
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("%s [%s]", super.toString(), score);
        }
    };

    public static abstract class Matcher {
        public abstract boolean match(Node node);
    };
}
