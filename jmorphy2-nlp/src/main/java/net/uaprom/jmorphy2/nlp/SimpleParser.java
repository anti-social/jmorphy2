package net.uaprom.jmorphy2.nlp;

import java.io.IOException;
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
import net.uaprom.jmorphy2.Parsed;
import net.uaprom.jmorphy2.Grammeme;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class SimpleParser {
    protected final MorphAnalyzer analyzer;
    protected final Ruleset rules;

    protected final Set<String> allowedGrammemeValues;

    public SimpleParser(MorphAnalyzer analyzer) throws IOException {
        this(analyzer, defaultRules);
    }
      
    public SimpleParser(MorphAnalyzer analyzer, Ruleset rules) throws IOException {
        this.analyzer = analyzer;
        this.rules = rules;

        this.allowedGrammemeValues = Sets.union(getGrammemeValuesFor(Tag.CASE),
                                                getGrammemeValuesFor(Tag.NUMBER));
    }

    private Set<String> getGrammemeValuesFor(String rootValue) {
        Set<String> values = new HashSet<String>();
        for (Grammeme grammeme : analyzer.getAllGrammemes()) {
            Grammeme rootGrammeme = grammeme.getRoot();
            if (rootGrammeme != null && rootGrammeme.equals(rootValue)) {
                values.add(grammeme.value);
            }
        }
        return values;
    }

    public Node.Top parse(List<Node.Top> sentences) {
        return parseAll(sentences).get(0);
    }

    public List<Node.Top> parseAll(List<Node.Top> sentences) {
        List<Node.Top> results = new ArrayList<Node.Top>();
        Set<Node.Top> uniqueTops = new HashSet<Node.Top>();
        
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
                            Node.Top top = new Node.Top(reducedNodes,
                                                        Node.calcScore(reducedNodes) / reducedNodes.size());
                            if (!uniqueTops.contains(top)) {
                                nextSentences.add(top);
                                uniqueTops.add(top);
                            }
                        }
                    }
                }

                if (!hasMatchedRules) {
                    results.add(sent);
                }
            }
            sentences = nextSentences;
        }

        Collections.sort(results, Node.scoreComparator());
        return results;
    }

    private ImmutableList<Node> reduce(Rule rule, ImmutableList<Node> nodes, int offset) {
        ImmutableList<Node> reducedNodes = nodes.subList(offset, offset + rule.rightSize);
        ImmutableList.Builder<Node> newNodesBuilder = ImmutableList.builder();
        newNodesBuilder.addAll(nodes.subList(0, offset));
        ImmutableSet<String> grammemeValues = rule.commonGrammemeValues(reducedNodes, allowedGrammemeValues);
        float score = (Node.calcScore(reducedNodes) + rule.weight) * grammemeValues.size();
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
        // defaultRules.add("S", "NP VP", 1000);
        // defaultRules.add("S", "NP", 900);
        defaultRules.add("NP", "NP @CONJ NP", 10);
        defaultRules.add("NP", "NP PP", 9);
        defaultRules.add("VP", "VP PP", 9);
        defaultRules.add("PP", "PREP NP", 8);
        defaultRules.add("NP", "NP,nomn NP,gent", 8);
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
        defaultRules.add("NP", "ADJF", 1);
        defaultRules.add("VP", "INFN", 1);
        defaultRules.add("VP", "VERB", 1);
    };
}
