package net.uaprom.jmorphy2.nlp;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Arrays;

import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import net.uaprom.jmorphy2.Tag;
import net.uaprom.jmorphy2.Parsed;
import net.uaprom.jmorphy2.Grammeme;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class SimpleParser {
    protected final MorphAnalyzer analyzer;
    protected final SimpleTagger tagger;
    protected final Rules rules;

    protected final Set<String> allowedGrammemeValues;

    public SimpleParser(MorphAnalyzer analyzer, SimpleTagger tagger) throws IOException {
        this(analyzer, tagger, defaultRules);
    }
      
    public SimpleParser(MorphAnalyzer analyzer, SimpleTagger tagger, Rules rules) throws IOException {
        this.analyzer = analyzer;
        this.tagger = tagger;
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
        SortedSet<Node.Top> results = new TreeSet<Node.Top>(Node.scoreComparator());
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
                        List<Rules.Rule> matchedRules = rules.matchAll(subNodes);
                        if (!matchedRules.isEmpty()) {
                            hasMatchedRules = true;
                        }
                        for (Rules.Rule rule : matchedRules) {
                            Node.Top top = new Node.Top(reduce(rule, nodes, offset),
                                                        (Node.calcScore(subNodes) + rule.weight) / subNodes.size());
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

        return new ArrayList<Node.Top>(results);
    }

    private ImmutableList<Node> reduce(Rules.Rule rule, ImmutableList<Node> nodes, int offset) {
        ImmutableList<Node> reducedNodes = nodes.subList(offset, offset + rule.rightSize);
        ImmutableList.Builder<Node> newNodesBuilder = ImmutableList.builder();
        newNodesBuilder.addAll(nodes.subList(0, offset));
        ImmutableSet<String> grammemeValues = commonGrammemeValues(rule.left, reducedNodes);
        float score = Node.calcScore(reducedNodes) * grammemeValues.size();
        newNodesBuilder.add(new Node(grammemeValues, reducedNodes, score));
        newNodesBuilder.addAll(nodes.subList(offset + rule.rightSize, nodes.size()));
        return newNodesBuilder.build();
    }

    private ImmutableSet<String> commonGrammemeValues(ImmutableSet<String> base, List<Node> nodes) {
        Set<String> values = null;
        for (Node node : nodes) {
            if (values == null) {
                values = node.grammemeValues;
            } else {
                values = Sets.intersection(values, node.grammemeValues);
            }
        }
        values = Sets.intersection(values, allowedGrammemeValues);
        values = Sets.union(values, base);
        return ImmutableSet.copyOf(values);
    }

    public static final Rules defaultRules = new Rules();
    static {
        // S - sentence
        // NP - noun phrase
        // VP - verb phrase
        // PP - preposition phrase
        // defaultRules.add("S", "NP VP", 1000);
        // defaultRules.add("S", "NP", 900);
        defaultRules.add("NP", "NP PP", 100);
        defaultRules.add("VP", "VP PP", 90);
        defaultRules.add("PP", "PREP NP", 50);
        defaultRules.add("PP", "PREP LATN", 50);
        defaultRules.add("PP", "PREP NUMB", 50);
        defaultRules.add("PP", "PP LATN", 75);
        defaultRules.add("PP", "PP NUMB", 75);
        defaultRules.add("NP", "NP CONJ NP", 500);
        defaultRules.add("NP", "NP,nomn NP,gent ", 200);
        defaultRules.add("NP", "ADJF NP", 100);
        defaultRules.add("NP", "NP ADJF", 50);
        defaultRules.add("VP", "INFN VERB", 10);
        defaultRules.add("NP", "NOUN", 3);
        defaultRules.add("NP", "ADJF", 2);
        defaultRules.add("VP", "INFN", 1);
        defaultRules.add("VP", "VERB", 1);
    };

    // public static class Rules {
    //     public static int PREPOSITION_MATHING_SCORE = 100;
    //     public static Map<String,String> PREPOSITION_MATHING = new HashMap<String,String>();
    //     static {
    //         PREPOSITION_MATHING.put(new Sequence("без(PREP)", "gent"), 1.0f);
    //         PREPOSITION_MATHING.put("без", "gent");
    //         PREPOSITION_MATHING.put("до", "gent");
    //         PREPOSITION_MATHING.put("для", "gent");
    //         PREPOSITION_MATHING.put("у", "gent");
    //         PREPOSITION_MATHING.put("ради", "gent");
    //         PREPOSITION_MATHING.put("к", "datv");
    //         PREPOSITION_MATHING.put("про", "accs");
    //         PREPOSITION_MATHING.put("через", "accs");
    //         PREPOSITION_MATHING.put("сквозь", "accs");
    //         PREPOSITION_MATHING.put("над", "ablt");
    //         PREPOSITION_MATHING.put("перед", "ablt");
    //         PREPOSITION_MATHING.put("при", "loct");
    //         PREPOSITION_MATHING.put("в", "accs|loct");
    //         PREPOSITION_MATHING.put("во", "accs|loct");
    //         PREPOSITION_MATHING.put("на", "accs|loct");
    //         PREPOSITION_MATHING.put("о", "accs|loct");
    //         PREPOSITION_MATHING.put("между", "gent|ablt");
    //         PREPOSITION_MATHING.put("за", "accs|ablt");
    //         PREPOSITION_MATHING.put("под", "accs|ablt");
    //         PREPOSITION_MATHING.put("по", "accs|datv|loct");
    //         PREPOSITION_MATHING.put("с", "gent|accs|ablt");
    //     };

    public static enum Grammar {
        SUBJECT,    // подлежащее
        PREDICATE,  // сказуемое
        MODIFIER,   // определение
        ADVERBIAL,  // обстоятельство
        APPOSITION, // приложение
        OBJECT,     // дополнение
    };
}
