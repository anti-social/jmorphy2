package net.uaprom.jmorphy2.nlp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;


@RunWith(JUnit4.class)
public class SimpleParserBenchmark extends BaseTestCase {
    private SimpleTagger tagger;
    private SimpleParser parser;

    public SimpleParserBenchmark() throws IOException {
        tagger = new SimpleTagger(analyzer);
        parser = new SimpleParser(analyzer);
    }

    @Test
    public void benchmarkParser() throws IOException {
        String[] words = new String[]{"уборка", "снега", "и", "вывоз", "льда"};
        long startTime, endTime;
        int N = 1000, WARMUP_CYCLES = 10;

        // warmup
        for (int i = 0; i < N * WARMUP_CYCLES; i++) {
            parser.parse(tagger.tagAll(words));
        }

        // 
        startTime = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            parser.parse(tagger.tagAll(words));
        }
        endTime = System.currentTimeMillis();
        System.out.println(String.format("SimpleParser.parse(tokens): %.1f sents/sec",
                                         ((float) N) / (endTime - startTime) * 1000));
        System.out.println("=======================");
    }
}
