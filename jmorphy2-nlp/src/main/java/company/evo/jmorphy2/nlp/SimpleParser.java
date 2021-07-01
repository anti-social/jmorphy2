package company.evo.jmorphy2.nlp;

import java.lang.Math;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import company.evo.jmorphy2.Tag;
import company.evo.jmorphy2.Grammeme;
import company.evo.jmorphy2.MorphAnalyzer;


public class SimpleParser extends Parser {
    protected final Ruleset rules;
    private final int threshold;

    protected final Set<String> allowedGrammemeValues;

    public static final int DEFAULT_THRESHOLD = 100;

    public SimpleParser(MorphAnalyzer morph, Tagger tagger) {
        this(morph, tagger, defaultRules);
    }

    public SimpleParser(MorphAnalyzer morph, Tagger tagger, Ruleset rules) {
        this(morph, tagger, rules, DEFAULT_THRESHOLD);
    }

    public SimpleParser(MorphAnalyzer morph, Tagger tagger, int threshold) {
        this(morph, tagger, defaultRules, threshold);
    }

    public SimpleParser(MorphAnalyzer morph, Tagger tagger, Ruleset rules, int threshold) {
        super(morph, tagger);
        this.rules = rules;
        this.threshold = threshold;
        Set<String> grammemeValues = new HashSet<>();
        grammemeValues.addAll(getGrammemeValuesFor(Tag.NUMBER));
        grammemeValues.addAll(getGrammemeValuesFor(Tag.CASE));
        this.allowedGrammemeValues = grammemeValues;
    }

    private Set<String> getGrammemeValuesFor(String rootValue) {
        Set<String> values = new HashSet<>();
        Grammeme rootGrammeme = morph.getGrammeme(rootValue);
        if (rootGrammeme == null) {
            return Collections.emptySet();
        }
        for (Grammeme grammeme : morph.getAllGrammemes()) {
            if (rootGrammeme.equals(grammeme.getRoot())) {
                values.add(grammeme.value);
            }
        }
        return values;
    }

    public Node.Top parse(List<Node.Top> sentences) {
        List<Node.Top> tops = parseAll(sentences);
        if (tops.isEmpty()) {
            return new Node.Top(List.of(), 0.0f);
        }
        return tops.get(0);
    }

    public List<Node.Top> parseAll(List<Node.Top> sentences) {
        // System.out.println(sentences.size());
        // int var = 0, wave = 0;
        List<Node.Top> results = new ArrayList<>();
        Set<Long> uniqueTopHashes = new HashSet<>();
        
        while (!sentences.isEmpty()) {
            List<Node.Top> nextSentences = new ArrayList<>();
            for (Node.Top sent : sentences) {
                boolean hasMatchedRules = false;
                List<Node> nodes = sent.getChildren();
                int nodesSize = sent.getChildrenSize();
                int minCount = 1;
                for (int offset = 0; offset <= nodesSize - minCount; offset++) {
                    for (int count = minCount; count <= nodesSize - offset; count++) {
                        List<Node> subNodes = nodes.subList(offset, offset + count);

                        Rule mRule = rules.match(subNodes);
                        if (mRule != null) {
                            hasMatchedRules = true;
                            List<Node> reducedNodes = reduce(mRule, nodes, offset);
                            float topScore = Node.sumScoreFor(reducedNodes) / reducedNodes.size() / (Node.maxDepthFor(reducedNodes) + 1);
                            Node.Top top = new Node.Top(reducedNodes, topScore);
                            if (!uniqueTopHashes.contains(top.uniqueHash)) {
                                nextSentences.add(top);
                                uniqueTopHashes.add(top.uniqueHash);
                            }
                            // var++;
                        }
                    }
                }

                if (!hasMatchedRules) {
                    results.add(sent);
                }
            }
            nextSentences.sort(Node.scoreComparator());
            sentences = nextSentences.subList(0, Math.min(threshold, nextSentences.size()));
            // wave++;
        }

        // System.out.println(wave);
        // System.out.println(var);
        // System.out.println(uniqueTopHashes.size());
        // System.out.println(results.size());
        results.sort(Node.scoreComparator());
        return results;
    }

    private List<Node> reduce(Rule rule, List<Node> nodes, int offset) {
        List<Node> newNodes = new ArrayList<>(nodes.subList(0, offset));

        List<Node> subNodes = nodes.subList(offset, offset + rule.rightSize);
        Set<String> grammemeValues = rule.commonGrammemeValues(subNodes, allowedGrammemeValues);
        float score = Node.sumScoreFor(subNodes) + rule.weight * grammemeValues.size();
        List<Node>  reducedNodes = new ArrayList<>();
        int i = 0;
        for (Node rNode : subNodes) {
            Rule.NodeMatcher m = rule.right.get(i);
            if (grammemeValues.equals(rNode.grammemeValues) &&
                (m.flags & Rule.NodeMatcher.NO_REDUCE) == 0) {
                reducedNodes.addAll(rNode.getChildren());
            } else {
                reducedNodes.add(rNode);
            }
            i++;
        }
        newNodes.add(new Node(grammemeValues, reducedNodes, score));
        newNodes.addAll(nodes.subList(offset + rule.rightSize, nodes.size()));

        return newNodes;
    }

    protected static final Ruleset defaultRules = new Ruleset();
    static {
        // S - sentence
        // NP - noun phrase
        // VP - verb phrase
        // PP - preposition phrase
        // @ - no common grammemes
        // $ - do not reduce
        // defaultRules.add("S", "NP VP", 1000);
        // defaultRules.add("S", "NP", 900);
        defaultRules.add("NP", "$NP @CONJ $NP", 10);
        defaultRules.add("NP", "NP PP", 9);
        defaultRules.add("VP", "VP PP", 9);
        defaultRules.add("PP", "PREP NP", 8);
        defaultRules.add("PP", "PP LATN", 8);
        defaultRules.add("PP", "PP NUMB", 8);
        defaultRules.add("PP", "PREP LATN", 8);
        defaultRules.add("PP", "PREP NUMB", 8);
        defaultRules.add("NP", "$NP,nomn @NP,gent", 8);
        defaultRules.add("NP", "NP @LATN", 5);
        defaultRules.add("NP", "NP @NUMB", 5);
        defaultRules.add("NP", "@LATN NP", 4);
        defaultRules.add("NP", "@NUMB NP", 4);
        defaultRules.add("NP", "ADJF NP", 9);
        defaultRules.add("NP", "NP ADJF", 8);
        defaultRules.add("VP", "INFN VERB", 2);
        defaultRules.add("UNKN", "Name | Erro", 2);
        defaultRules.add("NP", "NOUN,nomn", 2);
        defaultRules.add("NP", "NOUN,accs", 1);
        defaultRules.add("NP", "NOUN", 1);
        defaultRules.add("NP,nomn,sing", "LATN", 1);
        // defaultRules.add("NP", "ADJF", 1);
        defaultRules.add("VP", "INFN", 1);
        defaultRules.add("VP", "VERB", 1);
    }
}
