package net.uaprom.jmorphy2.nlp;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import net.uaprom.jmorphy2.MorphAnalyzer;
import net.uaprom.jmorphy2.Jmorphy2TestsHelpers;


@RunWith(JUnit4.class)
public class SimpleParserBenchmark {
    private MorphAnalyzer morph;
    private Parser parser;

    @Before
    public void setUp() throws IOException {
        morph = Jmorphy2TestsHelpers.newMorphAnalyzer();
        parser = new SimpleParser(morph, new SimpleTagger(morph));
    }

    @Test
    public void benchmarkParser() throws IOException {
        String[] words = new String[]{"уборка", "снега", "и", "вывоз", "льда"};
        long startTime, endTime;
        int N = 1000, WARMUP_CYCLES = 10;

        // warmup
        for (int i = 0; i < N * WARMUP_CYCLES; i++) {
            parser.parse(words);
        }

        // bench
        startTime = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            parser.parse(words);
        }
        endTime = System.currentTimeMillis();
        System.out.println(String.format("SimpleParser.parse(tokens): %.1f sents/sec",
                                         ((float) N) / (endTime - startTime) * 1000));
        System.out.println("=======================");
    }
}
