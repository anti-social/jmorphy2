package net.uaprom.jmorphy2.contrib;

import java.io.IOException;
import java.lang.Math;
import java.util.List;
import java.util.Deque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.base.Function;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Sets;
import com.google.common.collect.Ordering;
import com.google.common.collect.Iterables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import net.uaprom.jmorphy2.Tag;
import net.uaprom.jmorphy2.Parsed;
import net.uaprom.jmorphy2.Grammeme;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class SentenceParser {
    protected final MorphAnalyzer analyzer;
    protected final Tagger tagger;
    protected final Rules rules;
    protected final Set<String> partsOfSpeech;

    public SentenceParser(MorphAnalyzer analyzer, Tagger tagger) throws IOException {
        this(analyzer, tagger, defaultRules);
    }
      
    public SentenceParser(MorphAnalyzer analyzer, Tagger tagger, Rules rules) throws IOException {
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

    public List<Node> topParse(String[] tokens) throws IOException {
        float maxWeight = 0.0f;
        List<Node> nodes = null;
        MatchedRule bestMatchedRule = null;
        for (Tagger.Sequence seq : tagger.allSequences(tokens)) {
            nodes = makeNodes(seq);
            MatchedRule matchedRule = parse(nodes);
            if (matchedRule == null) {
                continue;
            }
            // float weight = matchedRule.weight * seq.score;
            float weight = matchedRule.weight;
            System.out.println("===================");
            System.out.println(matchedRule);
            System.out.println(seq.score);
            System.out.println(weight);
            System.out.println("===================");
            if (weight > maxWeight) {
                bestMatchedRule = matchedRule;
                maxWeight = weight;
            }
        }

        Deque<MatchedRule> matchedRules = new LinkedList<MatchedRule>();
        MatchedRule mRule = bestMatchedRule;
        while (mRule != null) {
            matchedRules.addFirst(mRule);
            mRule = mRule.parent;
        }
        System.out.println(matchedRules);
        return applyRules(matchedRules, nodes);
    }

    private List<Node> applyRules(Iterable<MatchedRule> matchedRules, List<Node> nodes) {
        for (MatchedRule mRule : matchedRules) {
            System.out.println(mRule);
            reduce(nodes, mRule);
        }
        return nodes;
    }

    private List<Node> makeNodes(Tagger.Sequence seq) {
        List<Node> nodes = new LinkedList<Node>();
        for (Parsed p : seq.parsedTokens) {
            nodes.add(new Word(p.tag.getGrammemeValues(), p.word));
        }
        return nodes;
    }

    private void reduce(List<Node> nodes, MatchedRule matchedRule) {
        List<Node> children = new ArrayList<Node>();
        for (int i = 0; i < matchedRule.count; i++) {
            children.add(nodes.remove(matchedRule.offset));
        }
        Node newNode = new Node(children, matchedRule.rule.left);
        nodes.add(matchedRule.offset, newNode);

        // List<Node> reducedNodes = new ArrayList<Node>();
        // for (int i = 0; i < matchedRule.offset; i++) {
        //     reducedNodes.add(nodes.get(i));
        // }
        // Node newNode = new Node(matchedRule.rule.left);
        // reducedNodes.add(matchedRule.offset, newNode);
        // for (int i = matchedRule.offset + matchedRule.count; i < nodes.size(); i++) {
        //     reducedNodes.add(nodes.get(i));
        // }
        // return reducedNodes;
    }
    
    private void expand(List<Node> nodes, MatchedRule matchedRule) {
        Node oldNode = nodes.remove(matchedRule.offset);
        nodes.addAll(matchedRule.offset, oldNode.children);
    }
    
    private MatchedRule parse(List<Node> nodes) {
        return parse(nodes, null);
    }

    private MatchedRule parse(List<Node> nodes, MatchedRule current) {
        System.out.println(nodes);
        MatchedRule bestMatchedRule = current;
        float parentWeight = current == null ? 0.0f : current.weight;
        for (MatchedRule mRule : matchAll(nodes)) {
            mRule.parent = current;
            mRule.weight = mRule.rule.weight + parentWeight;
            System.out.println(mRule);
            reduce(nodes, mRule);
            MatchedRule maybeBestRule = parse(nodes, mRule);
            System.out.println(maybeBestRule);
            expand(nodes, mRule);

            if (bestMatchedRule == null || maybeBestRule.weight > bestMatchedRule.weight) {
                bestMatchedRule = maybeBestRule;
            }
        }

        return bestMatchedRule;
    }

    private List<MatchedRule> matchAll(List<Node> nodes) {
        List<MatchedRule> matchedRules = new ArrayList<MatchedRule>();
        int maxCount = Math.min(rules.getLongestRuleCount(), nodes.size());

        for (int count = 1; count <= maxCount; count++) {
            for (int offset = 0; offset <= nodes.size() - count; offset++) {
                Rule rule = rules.match(nodes, offset, count);
                if (rule != null) {
                    matchedRules.add(new MatchedRule(rule, offset, count));
                }
            }
        }

        return matchedRules;
    }

    static class MatchedRule {
        public final Rule rule;
        public final int offset;
        public final int count;
        public float weight;
        public MatchedRule parent;
        public List<MatchedRule> children;

        public MatchedRule(Rule rule, int offset, int count) {
            this.rule = rule;
            this.offset = offset;
            this.count = count;
            this.children = new LinkedList<MatchedRule>();
        }

        @Override
        public String toString() {
            return String.format("%s, %s", rule, weight);
        }
    };

    public static class Rule {
        public final String origLeft;
        public final String origRight;
        public final Set<String> left;
        public final List<Set<String>> right;
        public final float weight;

        private final int[] flags;
        private static final int NO_COMMON = 0x01;

        public Rule(String left, String right, float weight) {
            this.origLeft = left;
            this.origRight = right;
            this.weight = weight;

            this.left = new HashSet<String>();
            for (String value : left.split(",")) {
                this.left.add(value);
            }

            this.right = new ArrayList<Set<String>>();
            String[] parts = right.split(" ");
            this.flags = new int[parts.length];
            int i = 0;
            for (String part : parts) {
                Set<String> clauses = new HashSet<String>();
                this.right.add(clauses);

                CharMatcher matcher = CharMatcher.is('@');
                if (matcher.matchesAnyOf(part)) {
                    part = matcher.removeFrom(part);
                    flags[i] |= NO_COMMON;
                }
                
                for (String value : part.split(",")) {
                    clauses.add(value);
                }

                i++;
            }
        }

        public boolean match(List<Node> nodes, int offset, int count) {
            int n = right.size();
            if (count != n) {
                return false;
            }

            for (int i = 0; i < count; i++) {
                if (!nodes.get(offset + i).grammemes.containsAll(right.get(i))) {
                    return false;
                }
            }

            return true;
        }

        // private Set<String> calculateCommon(List<Set<String>> candidate) {
        //     Set<String> common = null;
        //     for (int i = 0; i < right.size(); i++) {
        //         if ((flags[i] & NO_COMMON) != 0) {
        //             continue;
        //         }
        //         if (common == null) {
        //             common = candidate.get(i);
        //         }
        //         else {
        //             common = Sets.intersection(common, candidate.get(i));
        //         }
        //     }
        //     if (common == null) {
        //         return Collections.EMPTY_SET;
        //     }
        //     return common;
        // }

        @Override
        public String toString() {
            return String.format("%s -> %s [%s]", origLeft, origRight, weight);
        }
    }

    public static class Rules {
        public final List<Rule> rules = new ArrayList<Rule>();
        protected int longestRuleCount = 0;

        public void add(String left, String right, float weight) {
            Rule rule = new Rule(left, right, weight);
            rules.add(rule);
            longestRuleCount = Math.max(longestRuleCount, rule.right.size());
        }

        public int getLongestRuleCount() {
            return longestRuleCount;
        }

        public Rule match(List<Node> nodes, int offset, int count) {
            for (Rule rule : rules) {
                if (rule.match(nodes, offset, count)) {
                    return rule;
                }
            }
            return null;
        }
    };

    public static final Rules defaultRules = new Rules();
    static {
        // NP - noun phrase
        // defaultRules.add("S", "NP VP", 1000);
        // defaultRules.add("S", "NP", 900);
        defaultRules.add("NP", "NP PP", 100);
        defaultRules.add("VP", "VP PP", 90);
        defaultRules.add("PP", "PREP NP", 50);
        defaultRules.add("NP", "NP CONJ NP", 500);
        defaultRules.add("NP,nomn", "NP,nomn CONJ NP,nomn", 750);
        defaultRules.add("NP", "NP,nomn NP,gent ", 200);
        defaultRules.add("NP", "ADJF NP", 100);
        defaultRules.add("NP,nomn", "ADJF,nomn NP,nomn", 150);
        defaultRules.add("NP,gent", "ADJF,gent NP,gent", 120);
        defaultRules.add("NP", "NP ADJF", 50);
        defaultRules.add("NP,nomn", "NP,nomn ADJF,nomn", 75);
        defaultRules.add("NP,gent", "NP,gent ADJF,gent", 60);
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

    public static class Node {
        protected final List<Node> children;
        protected final Set<String> grammemes;

        public Node(List<Node> children, Set<String> grammemes) {
            this.children = children;
            this.grammemes = grammemes;
        }

        @Override
        public String toString() {
            return String.format("(%s %s)", grammemes, Joiner.on(" ").join(children));
        }
    }
    
    public static class Word extends Node {
        public final String word;

        public Word(Set<String> grammemes, String word) {
            super(null, grammemes);
            this.word = word;
        }

        @Override
        public String toString() {
            return String.format("(%s %s)", grammemes, word);
        }
    };
}
