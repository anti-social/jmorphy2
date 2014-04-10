package net.uaprom.jmorphy2.nlp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Deque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableList;

import net.uaprom.jmorphy2.Tag;
import net.uaprom.jmorphy2.Parsed;
import net.uaprom.jmorphy2.Grammeme;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class SimpleParser {
    protected final MorphAnalyzer analyzer;
    protected final SimpleTagger tagger;
    protected final Rules rules;
    protected final Set<String> partsOfSpeech;

    public SimpleParser(MorphAnalyzer analyzer, SimpleTagger tagger) throws IOException {
        this(analyzer, tagger, defaultRules);
    }
      
    public SimpleParser(MorphAnalyzer analyzer, SimpleTagger tagger, Rules rules) throws IOException {
        this.analyzer = analyzer;
        this.tagger = tagger;
        this.rules = rules;
        this.partsOfSpeech = getPartsOfSpeech(this.analyzer);
    }

    private Set<String> getPartsOfSpeech(MorphAnalyzer analyzer) {
        Set<String> partsOfSpeech = new HashSet<String>();
        for (Grammeme grammeme : analyzer.getAllGrammemes()) {
            if (grammeme.getRoot().equals(Tag.PART_OF_SPEECH)) {
                partsOfSpeech.add(grammeme.value);
            }
        }
        return partsOfSpeech;
    }

    public Node.Top parse(Node.Top[] sentences) {
        return parseAll(sentences)[0];
    }

    public Node.Top[] parseAll(Node.Top[] sentences) {
        return parseAll(Arrays.asList(sentences)).toArray(new Node.Top[0]);
    }

    private List<Node.Top> parseAll(List<Node.Top> sentences) {
        List<Node.Top> results = new ArrayList<Node.Top>();
        Set<Node.Top> uniqueTops = new HashSet<Node.Top>();
        
        while (!sentences.isEmpty()) {
            List<Node.Top> nextSentences = new ArrayList<Node.Top>();
            for (Node.Top sent : sentences) {
                boolean hasMatchedRules = false;
                ImmutableList<Node> nodes = sent.getChildren();
                int nodesSize = sent.getChildrenSize();
                for (int i = 0; i < nodesSize; i++) {
                    List<Rules.Rule> matchedRules = rules.matchAll(nodes.subList(i, nodesSize));
                    if (!matchedRules.isEmpty()) {
                        hasMatchedRules = true;
                    }
                    for (Rules.Rule rule : matchedRules) {
                        List<Node> subNodes = nodes.subList(i, i + rule.rightSize);
                        Node.Top top = new Node.Top(reduce(rule, nodes, i),
                                                    (Node.calcScore(subNodes) + rule.weight) / subNodes.size());
                        if (!uniqueTops.contains(top)) {
                            nextSentences.add(top);
                            uniqueTops.add(top);
                        }
                    }
                }

                if (!hasMatchedRules) {
                    results.add(sent);
                }
            }
            sentences = nextSentences;
        }

        Collections.sort(results, Collections.reverseOrder(Node.scoreComparator));
        return results;
    }

    private ImmutableList<Node> reduce(Rules.Rule rule, ImmutableList<Node> nodes, int offset) {
        ImmutableList<Node> reducedNodes = nodes.subList(offset, offset + rule.rightSize);
        ImmutableList.Builder<Node> newNodesBuilder = ImmutableList.builder();
        newNodesBuilder.addAll(nodes.subList(0, offset));
        newNodesBuilder.add(new Node(rule.left, reducedNodes, Node.calcScore(reducedNodes)));
        newNodesBuilder.addAll(nodes.subList(offset + rule.rightSize, nodes.size()));
        return newNodesBuilder.build();
    }

    public static final Rules defaultRules = new Rules();
    static {
        // S - sentence
        // NP - noun phrase
        // VP - verb phrase
        // defaultRules.add("S", "NP VP", 1000);
        // defaultRules.add("S", "NP", 900);
        defaultRules.add("NP", "NP PP", 100);
        defaultRules.add("VP", "VP PP", 90);
        defaultRules.add("PP", "PREP NP", 50);
        defaultRules.add("PP", "PREP LATN", 50);
        defaultRules.add("PP", "PREP NUMB", 50);
        defaultRules.add("PP", "PP LATN", 75);
        defaultRules.add("PP", "PP NUMB", 75);
        defaultRules.add("NP,nomn", "NP,nomn CONJ NP,nomn", 750);
        defaultRules.add("NP", "NP CONJ NP", 500);
        defaultRules.add("NP", "NP,nomn NP,gent ", 200);
        defaultRules.add("NP,nomn", "ADJF,nomn NP,nomn", 200);
        defaultRules.add("NP,gent", "ADJF,gent NP,gent", 150);
        defaultRules.add("NP", "ADJF NP", 100);
        defaultRules.add("NP,nomn", "NP,nomn ADJF,nomn", 100);
        defaultRules.add("NP,gent", "NP,gent ADJF,gent", 75);
        defaultRules.add("NP", "NP ADJF", 50);
        defaultRules.add("NP,nomn", "NOUN,nomn", 10);
        defaultRules.add("NP,gent", "NOUN,gent", 9);
        defaultRules.add("NP,nomn", "ADJF,nomn", 5);
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

    //     public static int CASE_MATHING_SCORE = 100;
    //     public static List<Rule> CASE_MATCHING = new ArrayList<Rule>();
    //     static {
    //         // CASE_MATHING.put(new Sequence("ADJF", "NOUN"), 1.5f);
    //         // CASE_MATHING.put(new Sequence("NOUN", "ADJF"), 1.0f);

    //         CASE_MATHING.add(new Rule("NP", "NP CONJ NP", 1000));
    //         CASE_MATHING.add(new Rule("NP", "ADJF NP", 1000));
    //         CASE_MATHING.add(new Rule("NP", "NP ADJF", 900));
    //         CASE_MATHING.add(new Rule("NP", "ADJF NOUN", 1000));
    //         CASE_MATHING.add(new Rule("NP", "NOUN ADJF", 400));
    //         CASE_MATHING.add(new Rule("NP", "NOUN,nomn NOUN,gent"), 100);
    //         CASE_MATHING.add(new Rule("NP", "NOUN,nomn", 50));
    //         CASE_MATHING.add(new Rule("NP", "ADJF,nomn", 40));
    //     };
    // };

    public static enum Grammar {
        SUBJECT,    // подлежащее
        PREDICATE,  // сказуемое
        MODIFIER,   // определение
        ADVERBIAL,  // обстоятельство
        APPOSITION, // приложение
        OBJECT,     // дополнение
    };
}
