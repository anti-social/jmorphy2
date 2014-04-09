package net.uaprom.jmorphy2.nlp;

import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;


public abstract class Rules {
    private final List<Rule> rules = new ArrayList<Rule>();

    private static Splitter rhsSplitter = Splitter.on("|").trimResults().omitEmptyStrings();

    public abstract Rule newRule(String left, String right, float weight);

    public void add(String left, String right) {
        add(left, right, 1.0f);
    }

    public void add(String left, String right, float weight) {
        for (String rightPart : rhsSplitter.split(right)) {
            rules.add(newRule(left, rightPart, weight));
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

    public static abstract class Rule {
        public final String origLeft;
        public final String origRight;
        public final ImmutableSet<String> left;
        public final ImmutableList<Node.Matcher> right;
        public final int rightSize;
        public final float weight;

        public Rule(String left, String right, float weight) {
            this.origLeft = left;
            this.origRight = right;
            this.left = parseLeft(left);
            this.right = parseRight(right);
            this.rightSize = this.right.size();
            this.weight = weight;
        }

        protected abstract ImmutableSet<String> parseLeft(String left);
            
        protected abstract ImmutableList<Node.Matcher> parseRight(String right);

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
            return new Node(nodes.subList(0, rightSize), left);
        }

        @Override
        public String toString() {
            return String.format("%s -> %s", origLeft, origRight);
        }
    }
}
