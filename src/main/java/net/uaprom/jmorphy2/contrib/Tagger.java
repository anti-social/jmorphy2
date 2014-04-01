package net.uaprom.jmorphy2.contrib;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.NavigableSet;
import java.util.Comparator;

import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;

import net.uaprom.jmorphy2.Parsed;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class Tagger {
    protected final MorphAnalyzer analyzer;

    public Tagger(MorphAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Sequence[] allSequences(String[] tokens) throws IOException {
        List<Set<Parsed>> parsedTokens = new ArrayList<Set<Parsed>>();
        for (String t : tokens) {
            parsedTokens.add(ImmutableSet.copyOf(analyzer.parse(t)));
        }

        NavigableSet<Sequence> sequences = new TreeSet<Sequence>(Sequence.comparator);
        for (List<Parsed> parseds : Sets.cartesianProduct(parsedTokens)) {
            float seqScore = 0;
            for (Parsed p : parseds) {
                seqScore += p.score;
            }
            sequences.add(new Sequence(parseds, seqScore));
        }

        return sequences.toArray(new Sequence[0]);
    }

    public Sequence topSequence(String[] tokens) throws IOException {
        List<Parsed> parsedTokens = new ArrayList<Parsed>();
        float seqScore = 0;
        for (String t : tokens) {
            List<Parsed> parseds = analyzer.parse(t);
            if (!parseds.isEmpty()) {
                Parsed p = parseds.get(0);
                parsedTokens.add(p);
                seqScore += p.score;
            }
        }
        return new Sequence(parsedTokens, seqScore);
    }

    public static class Sequence {
        public final List<Parsed> parsedTokens;
        public final float score;

        public static final Comparator<Sequence> comparator =
            new Comparator<Sequence>() {
                @Override
                public int compare(Sequence o1, Sequence o2) {
                    int res = Float.compare(o1.score, o2.score);
                    if (res == 0) {
                        if (o1.equals(o2)) {
                            return res;
                        }
                        return -1;
                    }
                    return res;
                }
            };
        
        public Sequence(List<Parsed> parsedTokens, float score) {
            this.parsedTokens = parsedTokens;
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("%s: %f", parsedTokens, score);
        }
    };
}
