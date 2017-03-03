package company.evo.jmorphy2;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;


@RunWith(JUnit4.class)
public class MorphAnalyzerBenchmark {
    private static final String WORDS_FREQ_RESOURCE =
        "/company/evo/jmorphy2/unigrams.txt";
    private static final int DEFAULT_REPEATS = 20;

    private MorphAnalyzer morph;
    private List<WordCount> words;
    private boolean initialized = false;

    @Before
    public void setUp() throws IOException {
        if (!initialized) {
            morph = Jmorphy2TestsHelpers.newMorphAnalyzer("ru");
            loadWords(WORDS_FREQ_RESOURCE);
        }
    }

    @Test
    public void benchmark() throws IOException {
        int repeats = DEFAULT_REPEATS;

        System.out.println("Benchmarking MorphAnalyzer:");
        benchParse(repeats);
    }

    public void benchParse(int repeats) throws IOException {
        long startTime = 0L, endTime = 0L;
        // List<List<ParsedWord>> res = new ArrayList<List<ParsedWord>>(words.size());

        for (int i = 0; i < repeats; i++) {
            int ix = 0;
            startTime = System.currentTimeMillis();
            for (WordCount word : words) {
                morph.parse(word.word);
                // res.add(ix, morph.parse(word.word));
                ix++;
            }
            endTime = System.currentTimeMillis();
        }

        printResults("MorphAnalyzer.parse(w)", endTime - startTime, words.size());
    }

    private void printResults(String name, long timeMillis, int count) {
        Float wps = ((float) count) / timeMillis * 1000;
        System.out.println(String.format("    %-50s %.1f words/sec", name, wps));
    }

    public void loadWords(String resource) throws IOException {
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
