package net.uaprom.jmorphy2.contrib;

import java.io.IOException;
import java.lang.Math;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import com.google.common.collect.Sets;

import net.uaprom.jmorphy2.MorphAnalyzer;
import net.uaprom.jmorphy2.Parsed;


class Phrase {
    public final String originalPhrase;
    protected final MorphAnalyzer analyzer;
    protected final Rules rules;

    public List<Node> nodes;
    
    public Phrase(String phrase, MorphAnalyzer analyzer) throws IOException {
        this(phrase, analyzer, defaultRules);
    }

    public Phrase(String phrase, MorphAnalyzer analyzer, Rules rules) throws IOException {
        this.originalPhrase = phrase;
        this.analyzer = analyzer;
        this.rules = rules;
        parse();
    }

    private void parse() throws IOException {
        nodes = new ArrayList<Node>();
        for (String w : originalPhrase.split(" ")) {
            nodes.add(new Word(w, analyzer.parse(w)));
        }

        int nodesLength = nodes.size();
        int n = Math.min(rules.getLongestRuleCount(), nodesLength);
        for (int i = n; i > 0; i--) {
            for (int offset = 0; offset <= nodesLength - i; offset++) {
                System.out.println(i);
                System.out.println(offset);
                for (List<Set<String>> candidate : Sets.cartesianProduct(getCandidates(nodes, offset, i))) {
                    System.out.println(candidate);
                    Rule rule = rules.match(candidate);
                    if (rule != null) {
                        // now reduce and next loop
                        Set<String> commonGrammemes = getCommonGrammemes(candidate);
                        // Node parentNode = new Node();
                        // parentNode.setChildren();
                        // parentNode.setGrammemes(commonGrammemes);
                        System.out.println(rule);
                        System.out.println(commonGrammemes);
                        System.out.println(rule.weight * commonGrammemes.size());
                    }
                }
            }
        }
    }

    private List<Set<Set<String>>> getCandidates(List<Node> nodes, int offset, int count) {
        List<Set<Set<String>>> candidates = new ArrayList<Set<Set<String>>>();
        for (int i = 0; i < count; i++) {
            candidates.add(nodes.get(offset + i).getAllGrammemes());
        }
        return candidates;
    }

    private Set<String> getCommonGrammemes(List<Set<String>> candidate) {
        if (candidate.size() >= 2) {
            Set<String> grammemes = Sets.intersection(candidate.get(0), candidate.get(1));
            for (int i = 2; i < candidate.size(); i++) {
                grammemes = Sets.intersection(grammemes, candidate.get(i));
            }
            return grammemes;
        }
        else if (candidate.size() == 1) {
            return candidate.get(0);
        }
        else {
            return Collections.EMPTY_SET;
        }
    }

    public static class Rule {
        public final String origLeft;
        public final String origRight;
        public final Set<String> left;
        public final List<Set<String>> right;
        public final float weight;

        public Rule(String left, String right, float weight) {
            this.origLeft = left;
            this.origRight = right;
            this.weight = weight;

            this.left = new HashSet<String>();
            for (String value : left.split(",")) {
                this.left.add(value);
            }

            this.right = new ArrayList<Set<String>>();
            for (String part : right.split(" ")) {
                Set<String> clauses = new HashSet<String>();
                this.right.add(clauses);
                for (String value : part.split(",")) {
                    clauses.add(value);
                }
            }
        }

        public boolean match(List<Set<String>> candidates) {
            int n = right.size();
            if (candidates.size() != n) {
                return false;
            }

            for (int i = 0; i < n; i++) {
                if (!candidates.get(i).containsAll(right.get(i))) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s", origLeft, origRight);
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

        public Rule match(List<Set<String>> candidates) {
            for (Rule rule : rules) {
                if (rule.match(candidates)) {
                    return rule;
                }
            }
            return null;
        }
    };

    public static final Rules defaultRules = new Rules();
    static {
        defaultRules.add("NP", "NP CONJ NP", 1000);
        defaultRules.add("NP", "ADJF NP", 1000);
        defaultRules.add("NP", "NP ADJF", 900);
        defaultRules.add("NP", "ADJF,nomn NOUN,nomn", 5000);
        defaultRules.add("NP", "ADJF NOUN", 1000);
        defaultRules.add("NP", "NOUN,nomn ADJF,nomn", 4000);
        defaultRules.add("NP", "NOUN ADJF", 400);
        defaultRules.add("NP", "NOUN,nomn NOUN,gent", 100);
        defaultRules.add("NP", "NOUN,nomn", 50);
        defaultRules.add("NP", "ADJF,nomn", 40);
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
        protected List<Node> children;
        protected Set<String> grammemes;
        // protected Grammar grammar;

        public void setChildren(List<Node> children) {
            this.children = children;
        }

        public List<Node> getChildren() {
            return children;
        }

        public void setGrammemes(Set<String> grammemes) {
            this.grammemes = grammemes;
        }

        public Set<String> getGrammemes() {
            return grammemes;
        }

        public Set<Set<String>> getAllGrammemes() {
            Set<Set<String>> allGrammemes = new HashSet<Set<String>>();
            allGrammemes.add(grammemes);
            return allGrammemes;
        }

        // public setGrammar(Grammar grammar) {
        //     this.grammar = grammar;
        // }
    }
    
    public static class Word extends Node {
        public final String word;
        public final List<Parsed> parseds;

        public Word(String word, List<Parsed> parseds) {
            this.word = word;
            this.parseds = parseds;
        }

        @Override
        public Set<Set<String>> getAllGrammemes() {
            Set<Set<String>> allGrammemes = new HashSet<Set<String>>();
            for (Parsed parsed : parseds) {
                allGrammemes.add(parsed.tag.getGrammemeValues());
            }
            return allGrammemes;
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass()) {
                return false;
            }

            Word other = (Word) obj;
            return word.equals(other.word);
        }
        
        @Override
        public String toString() {
            return String.format("{Word: %s}", word);
        }
    };
}
