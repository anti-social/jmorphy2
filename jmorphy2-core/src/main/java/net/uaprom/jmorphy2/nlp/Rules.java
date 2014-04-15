package net.uaprom.jmorphy2.nlp;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;


public class Rules {
    private final List<Rule> rules = new ArrayList<Rule>();
    private final Map<Integer,List<Rule>> rulesBySize = new HashMap<Integer,List<Rule>>();
    private int maxRightSize;

    private static final Splitter rhsSplitter = Splitter.on("|").trimResults().omitEmptyStrings();

    public void add(String left, String right) {
        add(left, right, 1.0f);
    }

    public void add(String left, String right, float weight) {
        for (String rightPart : rhsSplitter.split(right)) {
            Rule r = new Rule(left, rightPart, weight);
            rules.add(r);
            List<Rule> bySize = rulesBySize.get(r.rightSize);
            if (bySize == null) {
                bySize = new ArrayList<Rule>();
                rulesBySize.put(r.rightSize, bySize);
            }
            bySize.add(r);
            if (r.rightSize > maxRightSize) {
                maxRightSize = r.rightSize;
            }
        }
    }

    public int getMaxRightSize() {
        return maxRightSize;
    }

    public Rule match(List<Node> nodes) {
        List<Rule> testRules = rulesBySize.get(nodes.size());
        if (testRules == null) {
            return null;
        }
        for (Rule rule : testRules) {
            if (rule.match(nodes)) {
                return rule;
            }
        }
        return null;
    }

    public List<Rule> matchAll(List<Node> nodes) {
        List<Rule> matchedRules = new ArrayList<Rule>();
        List<Rule> testRules = rulesBySize.get(nodes.size());
        if (testRules == null) {
            return matchedRules;
        }
        for (Rule rule : testRules) {
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
        public final String leftStr;
        public final String rightStr;
        public final ImmutableSet<String> left;
        public final ImmutableList<NodeMatcher> right;
        public final int rightSize;
        public final float weight;

        private static Splitter grammemeSplitter = Splitter.on(",").trimResults(CharMatcher.anyOf("@!"));
        private static Splitter rhsSplitter = Splitter.on(" ").trimResults();
        private static CharMatcher wordMatcher = CharMatcher.anyOf("'\"");

        public Rule(String left, String right, float weight) {
            this.leftStr = left;
            this.rightStr = right;
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
                int flags = 0;
                if (part.startsWith("@")) {
                    flags |= NodeMatcher.NO_COMMONS;
                }
                if ((part.startsWith("'") && part.endsWith("'")) ||
                    (part.startsWith("\"") && part.endsWith("\""))) {
                    listBuilder.add(new NodeMatcher(null, wordMatcher.trimFrom(part), flags));
                } else {
                    listBuilder.add(new NodeMatcher(ImmutableSet.copyOf(grammemeSplitter.split(part)), null, flags));
                }
            }
            return listBuilder.build();
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

        public ImmutableSet<String> commonGrammemeValues(List<Node> nodes, Set<String> allowedValues) {
            Set<String> values = null;
            int i = 0;
            for (Node node : nodes) {
                if ((right.get(i).flags & NodeMatcher.NO_COMMONS) != 0) {
                    i++;
                    continue;
                }
                if (values == null) {
                    values = node.grammemeValues;
                } else {
                    values = Sets.intersection(values, node.grammemeValues);
                }
                i++;
            }
            values = Sets.intersection(values, allowedValues);
            values = Sets.union(values, left);
            return ImmutableSet.copyOf(values);
        }

        public Node apply(ImmutableList<Node> nodes) {
            ImmutableList<Node> reducedNodes = nodes.subList(0, rightSize);
            return new Node(left, reducedNodes, Node.calcScore(reducedNodes));
        }

        @Override
        public String toString() {
            return String.format("%s -> %s [%s]", leftStr, rightStr, weight);
        }

        public static class NodeMatcher {
            private final ImmutableSet<String> grammemeValues;
            private final int flags;
            private final String word;

            private final static int NO_COMMONS = 1;

            public NodeMatcher(ImmutableSet<String> grammemeValues, String word, int flags) {
                this.grammemeValues = grammemeValues;
                this.word = word;
                this.flags = flags;
            }

            public boolean match(Node node) {
                return node.match(grammemeValues, word);
            }
        };
    };
}
