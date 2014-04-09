package net.uaprom.jmorphy2.nlp;

import java.io.IOException;
import java.util.List;
import java.util.Deque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import net.uaprom.jmorphy2.Parsed;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class SimpleTagger {
    private final MorphAnalyzer analyzer;
    private final TaggerRules rules;

    public SimpleTagger(MorphAnalyzer analyzer) {
        this(analyzer, defaultRules);
    }

    public SimpleTagger(MorphAnalyzer analyzer, TaggerRules rules) {
        this.analyzer = analyzer;
        this.rules = rules;
    }

    public Node.Top[] tagAll(String[] tokens) throws IOException {
        List<Node.Top> results = new ArrayList<Node.Top>();
        tagAll(results, new LinkedList<Node>(), makeTokens(tokens));
        Collections.sort(results, Collections.reverseOrder(Node.comparator));
        return results.toArray(new Node.Top[0]);
    }

    private void tagAll(List<Node.Top> results, Deque<Node> nodesStack, ImmutableList<Node> nodes) throws IOException {
        List<Node> reducedNodes = new ArrayList<Node>();

        // test rules
        for (Rules.Rule rule : rules.matchAll(nodes)) {
            reducedNodes.add(rule.apply(nodes));
        }
        // parse word
        if (reducedNodes.isEmpty()) {
            Token tNode = (Token) nodes.get(0);
            List<Parsed> parseds = analyzer.parse(tNode.word);
            for (Parsed p : parseds) {
                reducedNodes.add(new Token(tNode.word,
                                           ImmutableSet.copyOf(p.tag.getGrammemeValues()),
                                           p.score));
            }
        }
        // leave as is
        if (reducedNodes.isEmpty()) {
            reducedNodes.add(nodes.get(0));
        }

        for (Node node : reducedNodes) {
            nodesStack.addLast(node);
            int offset = node.isLeaf() ? 1 : node.getChildren().size();
            ImmutableList<Node> tail = nodes.subList(offset, nodes.size());
            if (tail.isEmpty()) {
                float score = 0.0f;
                for (Node n : nodesStack) {
                    score += n.score;
                }
                results.add(new Node.Top(ImmutableList.copyOf(nodesStack), score));
            } else {
                tagAll(results, nodesStack, tail);
            }
            nodesStack.removeLast();
        }
    }

    public Node.Top tag(String[] tokens) throws IOException {
        ImmutableList<Node> tokenNodes = makeTokens(tokens);

        float score = 0.0f;
        int tokensSize = tokenNodes.size();
        ImmutableList.Builder<Node> nodesBuilder = ImmutableList.builder();
        int i = 0;
        while (i < tokensSize) {
            TaggerRule mRule = (TaggerRule) rules.match(tokenNodes);
            if (mRule != null) {
                nodesBuilder.add(mRule.apply(tokenNodes));
                score += mRule.weight;
                i += mRule.rightSize;
            } else {
                Token tNode = (Token) tokenNodes.get(i);
                List<Parsed> parseds = analyzer.parse(tNode.word);
                if (!parseds.isEmpty()) {
                    Parsed p = parseds.get(0);
                    nodesBuilder.add(new Token(tNode.word,
                                               ImmutableSet.copyOf(p.tag.getGrammemeValues()),
                                               p.score));
                    score += p.score;
                } else {
                    nodesBuilder.add(tokenNodes.get(i));
                }
                i++;
            }
        }
        return new Node.Top(nodesBuilder.build(), score);
    }

    private ImmutableList<Node> makeTokens(String[] words) {
        ImmutableList.Builder<Node> tokensBuilder = ImmutableList.builder();
        for (String w : words) {
            tokensBuilder.add(new Token(w, ImmutableSet.of("UNKN"), 1.0f));
        }
        return tokensBuilder.build();
    }

    public static class TaggerRules extends Rules {
        @Override
        public Rule newRule(String left, String right, float weight) {
            return new TaggerRule(left, right, weight);
        }
    }

    public static class TaggerRule extends Rules.Rule {
        private static Splitter lhsSplitter =
            Splitter.on(",").trimResults().omitEmptyStrings();
        private static Splitter rhsSplitter =
            Splitter.on(" ").trimResults(CharMatcher.anyOf(" '\"")).omitEmptyStrings();

        public TaggerRule(String left, String right, float weight) {
            super(left, right, weight);
        }
        
        @Override
        protected ImmutableSet<String> parseLeft(String left) {
            return ImmutableSet.copyOf(lhsSplitter.split(left));
        }

        @Override
        protected ImmutableList<Node.Matcher> parseRight(String right) {
            ImmutableList.Builder<Node.Matcher> listBuilder = ImmutableList.builder();
            for (String rightPart : rhsSplitter.split(right)) {
                listBuilder.add(new Token.Matcher(rightPart));
            }
            return listBuilder.build();
        }

        @Override
        public Node apply(ImmutableList<Node> nodes) {
            List<Node> rNodes = nodes.subList(0, rightSize);
            List<String> words = new ArrayList<String>();
            float score = 0.0f;
            for (Node node : rNodes) {
                Token t = (Token) node;
                words.add(t.word);
                score += t.score;
            }
            return new Token(Joiner.on(" ").join(words), left, score);
        }
    }

    public static final TaggerRules defaultRules = new TaggerRules();

    static {
        defaultRules.add("CONJ", "'а' | 'и' | 'но'");
        defaultRules.add("PREP",
                         "'без' | 'безо' | 'в' | 'во' | 'для' | 'до' | 'за' | " +
                         "'из' | 'из-за' | 'из-под' | 'к' | 'ко' | 'меж' | 'между' | " +
                         "'на' | 'над' | 'о' | 'об' | 'от' | 'перед' | 'по' | " +
                         "'при' | 'про' | 'с' | 'со' | 'у' | 'через'");
    }

    public static class Token extends Node {
        public final String word;

        public Token(String word, ImmutableSet<String> values, float score) {
            super(null, values, score);
            this.word = word;
        }

        @Override
        public String toString() {
            return String.format("(%s %s)", Joiner.on(",").join(values), word);
        }

        public static class Matcher extends Node.Matcher {
            private final String word;

            public Matcher(String word) {
                this.word = word;
            }

            @Override
            public boolean match(Node node) {
                if (node instanceof Token) {
                    return ((Token) node).word.equals(word);
                }
                return false;
            }
        }
    }
}
