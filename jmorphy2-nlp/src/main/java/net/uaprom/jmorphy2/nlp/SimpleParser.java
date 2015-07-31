package net.uaprom.jmorphy2.nlp;

import java.io.IOException;
import java.lang.Math;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import net.uaprom.jmorphy2.Tag;
import net.uaprom.jmorphy2.Grammeme;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class SimpleParser extends Parser {
    protected final Ruleset rules;
    private final int threshold;

    protected final Set<String> allowedGrammemeValues;

    public static final int DEFAULT_THRESHOLD = 100;

    public SimpleParser(MorphAnalyzer morph, Tagger tagger) throws IOException {
        this(morph, tagger, defaultRules);
    }
      
    public SimpleParser(MorphAnalyzer morph, Tagger tagger, Ruleset rules) throws IOException {
        this(morph, tagger, rules, DEFAULT_THRESHOLD);
    }
      
    public SimpleParser(MorphAnalyzer morph, Tagger tagger, int threshold) throws IOException {
        this(morph, tagger, defaultRules, threshold);
    }
      
    public SimpleParser(MorphAnalyzer morph, Tagger tagger, Ruleset rules, int threshold) throws IOException {
        super(morph, tagger);
        this.rules = rules;
        this.threshold = threshold;
        this.allowedGrammemeValues = Sets.union(getGrammemeValuesFor(Tag.CASE),
                                                getGrammemeValuesFor(Tag.NUMBER));
    }

    private Set<String> getGrammemeValuesFor(String rootValue) {
        Set<String> values = new HashSet<String>();
        for (Grammeme grammeme : morph.getAllGrammemes()) {
            Grammeme rootGrammeme = grammeme.getRoot();
            if (rootGrammeme != null && rootGrammeme.equals(rootValue)) {
                values.add(grammeme.value);
            }
        }
        return values;
    }

    public Node.Top parse(List<Node.Top> sentences) {
        List<Node.Top> tops = parseAll(sentences);
        if (tops.isEmpty()) {
            return new Node.Top(ImmutableList.<Node>of(), 0.0f);
        }
        return tops.get(0);
    }

    public List<Node.Top> parseAll(List<Node.Top> sentences) {
        // System.out.println(sentences.size());
        int var = 0, wave = 0;
        List<Node.Top> results = new ArrayList<Node.Top>();
        Set<Long> uniqueTopHashes = new HashSet<Long>();
        
        while (!sentences.isEmpty()) {
            List<Node.Top> nextSentences = new ArrayList<Node.Top>();
            for (Node.Top sent : sentences) {
                boolean hasMatchedRules = false;
                ImmutableList<Node> nodes = sent.getChildren();
                int nodesSize = sent.getChildrenSize();
                int minCount = 1;
                for (int offset = 0; offset <= nodesSize - minCount; offset++) {
                    for (int count = minCount; count <= nodesSize - offset; count++) {
                        List<Node> subNodes = nodes.subList(offset, offset + count);

                        Rule mRule = rules.match(subNodes);
                        if (mRule != null) {
                            hasMatchedRules = true;
                            ImmutableList<Node> reducedNodes = reduce(mRule, nodes, offset);
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
            Collections.sort(nextSentences, Node.scoreComparator());
            sentences = nextSentences.subList(0, Math.min(threshold, nextSentences.size()));
            // wave++;
        }

        // System.out.println(wave);
        // System.out.println(var);
        // System.out.println(uniqueTopHashes.size());
        // System.out.println(results.size());
        Collections.sort(results, Node.scoreComparator());
        return results;
    }

    private ImmutableList<Node> reduce(Rule rule, ImmutableList<Node> nodes, int offset) {
        ImmutableList.Builder<Node> newNodesBuilder = ImmutableList.builder();
        newNodesBuilder.addAll(nodes.subList(0, offset));

        ImmutableList<Node> subNodes = nodes.subList(offset, offset + rule.rightSize);
        ImmutableSet<String> grammemeValues = rule.commonGrammemeValues(subNodes, allowedGrammemeValues);
        float score = Node.sumScoreFor(subNodes) + rule.weight * grammemeValues.size();
        ImmutableList.Builder<Node>  reducedNodesBuilder = ImmutableList.builder();
        int i = 0;
        for (Node rNode : subNodes) {
            Rule.NodeMatcher m = rule.right.get(i);
            if (grammemeValues.equals(rNode.grammemeValues) &&
                (m.flags & Rule.NodeMatcher.NO_REDUCE) == 0) {
                reducedNodesBuilder.addAll(rNode.getChildren());
            } else {
                reducedNodesBuilder.add(rNode);
            }
            i++;
        }
        ImmutableList<Node> reducedNodes = reducedNodesBuilder.build();
        newNodesBuilder.add(new Node(grammemeValues, reducedNodes, score));

        newNodesBuilder.addAll(nodes.subList(offset + rule.rightSize, nodes.size()));

        return newNodesBuilder.build();
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
    };
}
