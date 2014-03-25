package net.uaprom.jmorphy2.contrib;

import java.io.IOException;
import java.lang.Math;
import java.util.List;
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
import com.google.common.collect.Iterables;

import net.uaprom.jmorphy2.Tag;
import net.uaprom.jmorphy2.Parsed;
import net.uaprom.jmorphy2.Grammeme;
import net.uaprom.jmorphy2.MorphAnalyzer;


class Phrase {
    public final String originalPhrase;
    protected final MorphAnalyzer analyzer;
    protected final Rules rules;
    protected final Set<String> partsOfSpeech;

    public List<Node> nodes;
    
    public Phrase(String phrase, MorphAnalyzer analyzer) throws IOException {
        this(phrase, analyzer, defaultRules);
    }

    public Phrase(String phrase, MorphAnalyzer analyzer, Rules rules) throws IOException {
        this.originalPhrase = phrase;
        this.analyzer = analyzer;
        this.rules = rules;
        this.partsOfSpeech = getPartsOfSpeech(this.analyzer);
        System.out.println(partsOfSpeech);
        parse();
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

    private void parse() throws IOException {
        nodes = new LinkedList<Node>();
        for (String w : originalPhrase.split(" ")) {
            nodes.add(new Word(w, analyzer.parse(w)));
        }
        System.out.println(nodes);

        int count = Math.min(rules.getLongestRuleCount(), nodes.size());
        while (count > 0) {
            System.out.println(count);
            int nodesLength = nodes.size();
            MatchedRule bestMatchedRule = null;
            for (int offset = 0; offset <= nodesLength - count; offset++) {
                for (List<Set<String>> candidate : Sets.cartesianProduct(getCandidates(nodes, offset, count))) {
                    // System.out.println(candidate);
                    MatchedRule rule = rules.match(candidate, new MatchedMeta(offset, count));
                    System.out.println(rule);
                    if (rule != null && (bestMatchedRule == null || rule.weight > bestMatchedRule.weight)) {
                        bestMatchedRule = rule;
                    }
                }
            }

            // System.out.println(bestMatchedRule);
            if (bestMatchedRule == null) {
                count--;
            }
            else {
                System.out.println(bestMatchedRule.rule);
                List<Node> children = new ArrayList<Node>();
                for (int i = 0; i < bestMatchedRule.meta.count; i++) {
                    children.add(nodes.remove(bestMatchedRule.meta.offset));
                }
                Node parentNode = new Node();
                parentNode.setChildren(children);
                parentNode.setGrammemes(Sets.difference(bestMatchedRule.common, partsOfSpeech));
                nodes.add(bestMatchedRule.meta.offset, parentNode);
                count = Math.min(rules.getLongestRuleCount(), nodes.size());
                System.out.println(nodes);
                System.out.println("================");
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

    static class MatchedMeta {
        public final int offset;
        public final int count;

        public MatchedMeta(int offset, int count) {
            this.offset = offset;
            this.count = count;
        }
    };

    static class MatchedRule {
        public final Rule rule;
        public final MatchedMeta meta;
        public final Set<String> common;
        public final float weight;

        public MatchedRule(Rule rule, MatchedMeta meta, Set<String> common, float weight) {
            this.rule = rule;
            this.meta = meta;
            this.common = common;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return String.format("%s, %s, %s", rule, common, weight);
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

        public MatchedRule match(List<Set<String>> candidate) {
            return match(candidate, null);
        }

        public MatchedRule match(List<Set<String>> candidate, MatchedMeta meta) {
            int n = right.size();
            if (candidate.size() != n) {
                return null;
            }

            for (int i = 0; i < n; i++) {
                if (!candidate.get(i).containsAll(right.get(i))) {
                    return null;
                }
            }

            Set<String> common = calculateCommon(candidate);
            common = Sets.union(common, left);
            return new MatchedRule(this, meta, common, weight + common.size());
        }

        private Set<String> calculateCommon(List<Set<String>> candidate) {
            Set<String> common = null;
            for (int i = 0; i < right.size(); i++) {
                if ((flags[i] & NO_COMMON) != 0) {
                    continue;
                }
                if (common == null) {
                    common = candidate.get(i);
                }
                else {
                    common = Sets.intersection(common, candidate.get(i));
                }
            }
            if (common == null) {
                return Collections.EMPTY_SET;
            }
            return common;
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

        public MatchedRule match(List<Set<String>> candidate) {
            return match(candidate, null);
        }

        public MatchedRule match(List<Set<String>> candidate, MatchedMeta meta) {
            for (Rule rule : rules) {
                MatchedRule matchedRule = rule.match(candidate, meta);
                if (matchedRule != null) {
                    return matchedRule;
                }
            }
            return null;
        }
    };

    public static final Rules defaultRules = new Rules();
    static {
        // NP - noun phrase
        defaultRules.add("NP", "NP @CONJ NP", 100000);
        defaultRules.add("NP", "NP @CONJ NOUN", 90000);
        defaultRules.add("NP", "NOUN @CONJ NP", 90000);
        defaultRules.add("NP", "NOUN @CONJ NOUN", 80000);
        defaultRules.add("NP", "NP,nomn NP,gent ", 5000);
        defaultRules.add("NP", "ADJF NP", 1000);
        defaultRules.add("NP", "NP ADJF", 900);
        defaultRules.add("NP", "ADJF,nomn NOUN,nomn", 5000);
        defaultRules.add("NP", "ADJF NOUN", 1000);
        // defaultRules.add("NP", "NP,nomn NOUN,gent", 4000);
        defaultRules.add("NP", "NOUN,nomn ADJF,nomn", 4000);
        defaultRules.add("NP", "NOUN ADJF", 400);
        defaultRules.add("NP", "NOUN,nomn", 50);
        defaultRules.add("NP", "ADJF,nomn", 40);
        defaultRules.add("NP", "NOUN", 10);
        defaultRules.add("NP", "ADJF", 9);
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

        @Override
        public String toString() {
            // return String.format("{Node: %s, %s}",
            //                      grammemes,
            //                      children);
            return toString(0);
        }

        public String toString(final int level) {
            String childNodes;
            if (children != null) {
                childNodes = Joiner.on(", ").join(Iterables.transform(children, new Function<Node,String>() {
                            public String apply(Node node) {
                                return node.toString(level + 1);
                            }
                        }));
            } else {
                childNodes = "";
            }
            return String.format("\n%s{Node: %s, %s}",
                                 Strings.repeat(" ", level * 2),
                                 grammemes,
                                 childNodes);
        }
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
            return toString(0);
        }

        @Override
        public String toString(final int level) {
            return String.format("\n%s{Word: %s}",
                                 Strings.repeat(" ", 2 * level),
                                 word);
        }
    };
}
