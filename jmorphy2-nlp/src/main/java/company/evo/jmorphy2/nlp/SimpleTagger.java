package company.evo.jmorphy2.nlp;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Deque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;

import company.evo.jmorphy2.ParsedWord;
import company.evo.jmorphy2.MorphAnalyzer;


public class SimpleTagger extends Tagger {
    protected final Ruleset rules;
    private final int threshold;

    public static final int DEFAULT_THRESHOLD = 1000;

    public SimpleTagger(MorphAnalyzer morph) {
        this(morph, defaultRules);
    }

    public SimpleTagger(MorphAnalyzer morph, Ruleset rules) {
        this(morph, rules, DEFAULT_THRESHOLD);
    }

    public SimpleTagger(MorphAnalyzer morph, int threshold) {
        this(morph, defaultRules, threshold);
    }

    public SimpleTagger(MorphAnalyzer morph, Ruleset rules, int threshold) {
        super(morph);
        this.rules = rules;
        this.threshold = threshold;
    }

    public List<Node.Top> tagAll(String[] tokens) throws IOException {
        List<Node.Top> results = new ArrayList<Node.Top>();
        if (tokens.length != 0) {
            tagAll(results, new LinkedList<Node>(), makeTokens(tokens));
            Collections.sort(results, Collections.reverseOrder(Node.scoreComparator()));
        }
        return results;
    }

    private Node reduce(Rule rule, List<Node> nodes) {
        List<Node> rNodes = nodes.subList(0, rule.rightSize);
        List<String> words = new ArrayList<String>();
        float score = 0.0f;
        for (Node node : rNodes) {
            words.add(node.word);
            score += node.score;
        }
        return new Node(rule.left, String.join(" ", words), score);
    }

    private void tagAll(List<Node.Top> results, Deque<Node> nodesStack, List<Node> nodes) throws IOException {
        List<Node> reducedNodes = new ArrayList<Node>();

        // test rules
        for (int count = 1; count <= rules.getMaxRightSize(); count++) {
            for (Rule rule : rules.matchAll(nodes.subList(0, count))) {
                reducedNodes.add(reduce(rule, nodes));
            }
        }
        // parse word
        if (reducedNodes.isEmpty()) {
            Node tNode = nodes.get(0);
            List<ParsedWord> parseds = morph.parse(tNode.word);
            for (ParsedWord p : parseds) {
                reducedNodes.add(new Node(Set.copyOf(p.tag.getGrammemeValues()),
                                          p,
                                          p.score));
                if (p.tag.contains("Fixd")) {
                    break;
                }
            }
        }
        // unknown word
        if (reducedNodes.isEmpty()) {
            reducedNodes.add(nodes.get(0));
        }

        if (!reducedNodes.isEmpty() && results.size() > threshold) {
            reducedNodes = reducedNodes.subList(0, 1);
        }

        for (Node node : reducedNodes) {
            nodesStack.addLast(node);
            int offset = node.hasChildren() ? node.getChildren().size() : 1;
            List<Node> tail = nodes.subList(offset, nodes.size());
            if (tail.isEmpty()) {
                float score = 0.0f;
                for (Node n : nodesStack) {
                    score += n.score;
                }
                results.add(new Node.Top(List.copyOf(nodesStack), score));
            } else {
                tagAll(results, nodesStack, tail);
            }
            nodesStack.removeLast();
        }
    }

    public Node.Top tag(String[] tokens) throws IOException {
        List<Node> tokenNodes = makeTokens(tokens);

        float score = 0.0f;
        int tokensSize = tokenNodes.size();
        List<Node> nodes = new ArrayList<Node>();
        int i = 0;
        while (i < tokensSize) {
            Rule mRule = rules.match(tokenNodes);
            if (mRule != null) {
                nodes.add(reduce(mRule, tokenNodes));
                score += mRule.weight;
                i += mRule.rightSize;
            } else {
                Node tNode = tokenNodes.get(i);
                List<ParsedWord> parseds = morph.parse(tNode.word);
                if (!parseds.isEmpty()) {
                    ParsedWord p = parseds.get(0);
                    nodes.add(new Node(Set.copyOf(p.tag.getGrammemeValues()),
                                              p,
                                              p.score));
                    score += p.score;
                } else {
                    nodes.add(tokenNodes.get(i));
                }
                i++;
            }
        }
        return new Node.Top(nodes, score);
    }

    private List<Node> makeTokens(String[] words) {
        List<Node> tokens = new ArrayList<Node>();
        for (String w : words) {
            tokens.add(new Node(Set.of("UNKN"), w, 1.0f));
        }
        return tokens;
    }

    public static final Ruleset defaultRules = new Ruleset();
    static {
        defaultRules.add("CONJ", "'а' | 'и' | 'но'");
        defaultRules.add("PREP",
                         "'без' | 'безо' | 'в' | 'во' | 'для' | 'до' | 'за' | " +
                         "'из' | 'из-за' | 'из-под' | 'к' | 'ко' | 'меж' | 'между' | " +
                         "'на' | 'над' | 'о' | 'об' | 'от' | 'перед' | 'по' | " +
                         "'при' | 'про' | 'с' | 'со' | 'у' | 'через'");
    }
}
