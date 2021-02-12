package company.evo.jmorphy2.nlp;

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
public class SubjectExtractorBenchmark extends SubjectExtractorTest {
    private static final String PHRASES_RESOURCE = "/phrases.txt";
    private static final int DEFAULT_REPEATS = 5;

    private List<String[]> phrases;

    @Before
    public void loadResources() throws IOException {
        loadPhrases(PHRASES_RESOURCE);
    }

    @Test
    public void benchmark() throws IOException {
        int repeats = DEFAULT_REPEATS;

        System.out.println("Benchmarking SubjectExtractor:");
        benchExtract(repeats);
    }

    public void benchExtract(int repeats) throws IOException {
        long startTime = 0L, endTime = 0L;
        // List<List<Parsed>> res = new ArrayList<List<Parsed>>(words.size());

        for (int i = 0; i < repeats; i++) {
            int ix = 0;
            startTime = System.currentTimeMillis();
            for (String[] phrase : phrases) {
                List<String> subj = subjExtractor.extract(phrase);
                ix++;
            }
            endTime = System.currentTimeMillis();
        }

        printResults("SubjectExtractor.extract(w)", endTime - startTime, phrases.size());
    }

    private void printResults(String name, long timeMillis, int count) {
        Float wps = ((float) count) / timeMillis * 1000;
        System.out.println(String.format("    %-50s %.1f phrases/sec", name, wps));
    }

    public void loadPhrases(String resource) throws IOException {
        phrases = new ArrayList<String[]>();
        InputStream stream = getClass().getResourceAsStream(resource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String rawPhrase;
        while ((rawPhrase = reader.readLine()) != null) {
            List<String> words = new ArrayList<String>();
            for (String w : rawPhrase.split("[\\p{Punct}\\t ]")) {
                if (!"".equals(w)) {
                    words.add(w);
                }
            }
            phrases.add(words.toArray(new String[0]));
        }
        System.out.println(String.format("Loaded %d phrases", phrases.size()));
    }
}
