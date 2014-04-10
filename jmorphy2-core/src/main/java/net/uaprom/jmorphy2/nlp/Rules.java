package net.uaprom.jmorphy2.nlp;

import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;


public class Rules {
    private final List<Rule> rules = new ArrayList<Rule>();

    private static Splitter rhsSplitter = Splitter.on("|").trimResults().omitEmptyStrings();

    public void add(String left, String right) {
        add(left, right, 1.0f);
    }

    public void add(String left, String right, float weight) {
        for (String rightPart : rhsSplitter.split(right)) {
            rules.add(new Rule(left, rightPart, weight));
        }
    }

    public Rule match(List<Node> nodes) {
        for (Rule rule : rules) {
            if (rule.match(nodes)) {
                return rule;
            }
        }
        return null;
    }

    public List<Rule> matchAll(List<Node> nodes) {
        List<Rule> matchedRules = new ArrayList<Rule>();
        for (Rule rule : rules) {
            if (rule.match(nodes)) {
                matchedRules.add(rule);
            }
        }
        return matchedRules;
    }

    @Override
    public String toString() {
        return Joiner.on("\n").join(rules);
    }

    public static class Rule {
        public final String origLeft;
        public final String origRight;
        public final ImmutableSet<String> left;
        public final ImmutableList<NodeMatcher> right;
        public final int rightSize;
        public final float weight;

        private static Splitter grammemeSplitter = Splitter.on(",");
        private static Splitter rhsSplitter = Splitter.on(" ").trimResults();
        private static CharMatcher wordMatcher = CharMatcher.anyOf("'\"");

        public Rule(String left, String right, float weight) {
            this.origLeft = left;
            this.origRight = right;
            this.left = parseLeft(left);
            this.right = parseRight(right);
            this.rightSize = this.right.size();
            this.weight = weight;
        }

        protected ImmutableSet<String> parseLeft(String left) {
            return ImmutableSet.copyOf(grammemeSplitter.split(left));
        }

        protected ImmutableList<NodeMatcher> parseRight(String right) {
            ImmutableList.Builder<NodeMatcher> listBuilder = ImmutableList.builder();
            for (String part : rhsSplitter.split(right)) {
                if ((part.startsWith("'") && part.endsWith("'")) ||
                    (part.startsWith("\"") && part.endsWith("\""))) {
                    listBuilder.add(new NodeMatcher(null, wordMatcher.trimFrom(part)));
                } else {
                    listBuilder.add(new NodeMatcher(ImmutableSet.copyOf(grammemeSplitter.split(part)), null));
                }
            }
            return listBuilder.build();
        }

        public boolean match(List<Node> nodes) {
            int n = right.size();
            if (nodes.size() < n) {
                return false;
            }

            for (int i = 0; i < n; i++) {
                if (!right.get(i).match(nodes.get(i))) {
                    return false;
                }
            }
            return true;
        }

        public Node apply(ImmutableList<Node> nodes) {
            ImmutableList<Node> reducedNodes = nodes.subList(0, rightSize);
            return new Node(left, reducedNodes, Node.calcScore(reducedNodes));
        }

        @Override
        public String toString() {
            return String.format("%s -> %s", origLeft, origRight);
        }

        public static class NodeMatcher {
            private final ImmutableSet<String> grammemeValues;
            private final String word;

            public NodeMatcher(ImmutableSet<String> grammemeValues, String word) {
                this.grammemeValues = grammemeValues;
                this.word = word;
            }

            public boolean match(Node node) {
                return node.match(grammemeValues, word);
            }
        };
    };
}
