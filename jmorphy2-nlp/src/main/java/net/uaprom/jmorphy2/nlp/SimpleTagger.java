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
    private final Rules rules;

    public SimpleTagger(MorphAnalyzer analyzer) {
        this(analyzer, defaultRules);
    }

    public SimpleTagger(MorphAnalyzer analyzer, Rules rules) {
        this.analyzer = analyzer;
        this.rules = rules;
    }

    public List<Node.Top> tagAll(String[] tokens) throws IOException {
        List<Node.Top> results = new ArrayList<Node.Top>();
        tagAll(results, new LinkedList<Node>(), makeTokens(tokens));
        Collections.sort(results, Collections.reverseOrder(Node.scoreComparator()));
        return results;
    }

    private Node reduce(Rules.Rule rule, ImmutableList<Node> nodes) {
        List<Node> rNodes = nodes.subList(0, rule.rightSize);
        List<String> words = new ArrayList<String>();
        float score = 0.0f;
        for (Node node : rNodes) {
            words.add(node.word);
            score += node.score;
        }
        return new Node(rule.left, Joiner.on(" ").join(words), score);
    }

    private void tagAll(List<Node.Top> results, Deque<Node> nodesStack, ImmutableList<Node> nodes) throws IOException {
        List<Node> reducedNodes = new ArrayList<Node>();

        // test rules
        for (int count = 1; count <= rules.getMaxRightSize(); count++) {
            for (Rules.Rule rule : rules.matchAll(nodes.subList(0, count))) {
                reducedNodes.add(reduce(rule, nodes));
            }
        }
        // parse word
        if (reducedNodes.isEmpty()) {
            Node tNode = nodes.get(0);
            List<Parsed> parseds = analyzer.parse(tNode.word);
            for (Parsed p : parseds) {
                reducedNodes.add(new Node(ImmutableSet.copyOf(p.tag.getGrammemeValues()),
                                          p,
                                          p.score));
            }
        }
        // unknown word
        if (reducedNodes.isEmpty()) {
            reducedNodes.add(nodes.get(0));
        }

        for (Node node : reducedNodes) {
            nodesStack.addLast(node);
            int offset = node.hasChildren() ? node.getChildren().size() : 1;
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
            Rules.Rule mRule = rules.match(tokenNodes);
            if (mRule != null) {
                nodesBuilder.add(reduce(mRule, tokenNodes));
                score += mRule.weight;
                i += mRule.rightSize;
            } else {
                Node tNode = tokenNodes.get(i);
                List<Parsed> parseds = analyzer.parse(tNode.word);
                if (!parseds.isEmpty()) {
                    Parsed p = parseds.get(0);
                    nodesBuilder.add(new Node(ImmutableSet.copyOf(p.tag.getGrammemeValues()),
                                              p,
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
            tokensBuilder.add(new Node(ImmutableSet.of("UNKN"), w, 1.0f));
        }
        return tokensBuilder.build();
    }

    public static final Rules defaultRules = new Rules();
    static {
        defaultRules.add("CONJ", "'а' | 'и' | 'но'");
        defaultRules.add("PREP",
                         "'без' | 'безо' | 'в' | 'во' | 'для' | 'до' | 'за' | " +
                         "'из' | 'из-за' | 'из-под' | 'к' | 'ко' | 'меж' | 'между' | " +
                         "'на' | 'над' | 'о' | 'об' | 'от' | 'перед' | 'по' | " +
                         "'при' | 'про' | 'с' | 'со' | 'у' | 'через'");
    }
}
