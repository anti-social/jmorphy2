package net.uaprom.jmorphy2.nlp;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Arrays;

import net.uaprom.jmorphy2.MorphAnalyzer;
import net.uaprom.jmorphy2.BaseTestCase;


@RunWith(JUnit4.class)
public class SimpleParserTest extends BaseTestCase {
    private SimpleTagger tagger;
    private SimpleParser parser;

    public SimpleParserTest() throws IOException {
        tagger = new SimpleTagger(analyzer);
        parser = new SimpleParser(analyzer, tagger);
    }

    @Test
    public void testTagger() throws IOException {
        System.out.println(tagger.tag(new String[]{"женские", "сапоги"}));
        System.out.println(tagger.tag(new String[]{"женские", "сапоги", "на", "зиму"}));
        System.out.println(tagger.tag(new String[]{"чехол", "для", "iphone", "4s"}));
        System.out.println(tagger.tag(new String[]{"шуруповерт", "Bosch"}));
        System.out.println("=======================");

        // for (Node top : tagger.tagAll(new String[]{"уборка", "и", "вывоз", "снега", "и", "льда"})) {
        //     System.out.println(top);
        // }
        // System.out.println("=======================");
    }

    @Test
    public void testParser() throws IOException {
        // assertEquals("(TOP (NP,nomn,plur (ADJF,nomn,plur женские) ((NP,nomn,plur (NOUN,nomn,plur сапоги)))))",
        //              parser.parse(tagger.tagAll(new String[]{"женские", "сапоги"})).toString());
        
        System.out.println(parser.parse(tagger.tagAll(new String[]{"женские", "сапоги"})));
        System.out.println(parser.parse(tagger.tagAll(new String[]{"чехол", "для", "телефона"})));
        System.out.println(parser.parse(tagger.tagAll(new String[]{"чехол", "для", "iphone", "5"})));
        System.out.println(parser.parse(tagger.tagAll(new String[]{"женские", "сапоги", "коричневые"})));
        System.out.println(parser.parse(tagger.tagAll(new String[]{"уборка", "и", "вывоз", "снега", "и", "льда"})));
        System.out.println(parser.parse(tagger.tagAll(new String[]{"уборка", "снега", "и", "вывоз", "льда"})));
        System.out.println("=======================");

        // for (Node top : parser.parseAll(tagger.tagAll(new String[]{"уборка", "снега", "и", "вывоз", "льда"}))) {
        //     System.out.println(top);
        // }
        // System.out.println("=======================");
    }

    // @Test
    public void benchmarkParser() throws IOException {
        String[] words = new String[]{"уборка", "снега", "и", "вывоз", "льда"};
        long startTime = 0L, endTime = 0L;
        int N = 1000;
        // warmup
        for (int i = 0; i < N * 20; i++) {
            parser.parse(tagger.tagAll(words));
        }
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
