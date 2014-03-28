package net.uaprom.jmorphy2;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;


@RunWith(JUnit4.class)
public class BenchmarkAnalyzer {
    private static final String WORDS_FREQ_RESOURCE = "/unigrams.txt";
    private static final int DEFAULT_REPEATS = 5;
    private static final int DEFAULT_WARMUP_REPEATS = 50;

    private MorphAnalyzer analyzer;
    private List<WordCount> words;

    public BenchmarkAnalyzer() throws IOException {
        Map<Character,String> replaceChars = new HashMap<Character,String>();
        replaceChars.put('е', "ё");
        analyzer = new MorphAnalyzer(replaceChars);
        loadWords(WORDS_FREQ_RESOURCE);
    }

    @Test
    public void benchmark() throws IOException {
        int repeats = DEFAULT_REPEATS;
        int warmupRepeats = DEFAULT_WARMUP_REPEATS;

        System.out.println("Benchmarking MorphAnalyzer:");
        benchParse(repeats, warmupRepeats);
    }

    public void benchParse(int repeats, int warmupRepeats) throws IOException {
        benchParse(warmupRepeats, true);
        benchParse(repeats, false);
    }

    public void benchParse(int repeats, boolean isWarmup) throws IOException {
        long startTime, endTime;
        int count = repeats * words.size();
        // List<List<Parsed>> res = new ArrayList<List<Parsed>>(words.size());

        startTime = System.currentTimeMillis();
        for (int i = 0; i < repeats; i++) {
            int ix = 0;
            for (WordCount word : words) {
                analyzer.parse(word.word);
                // res.add(ix, analyzer.parse(word.word));
                ix++;
            }
        }
        endTime = System.currentTimeMillis();
        if (!isWarmup) {
            printResults("MorphAnalyzer.parse(w)", endTime - startTime, count);
        }
    }

    private void printResults(String name, long timeMillis, int count) {
        Float wps = ((float) count) / timeMillis * 1000;
        System.out.println(String.format("    %-50s %.1f words/sec", name, wps));
    }

    private void loadWords(String resource) throws IOException {
        words = new ArrayList<WordCount>();
        InputStream stream = getClass().getResourceAsStream(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\s");
            if (parts.length < 2) {
                continue;
            }
            words.add(new WordCount(parts[0].toLowerCase(), Integer.valueOf(parts[1])));
        }
        System.out.println(String.format("Loaded %d words", words.size()));
    }

    static class WordCount {
        public final String word;
        public final int count;

        public WordCount(String word, int count) {
            this.word = word;
            this.count = count;
        }
    }
}
