package net.uaprom.jmorphy2.nlp;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.Arrays;

import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import net.uaprom.jmorphy2.MorphAnalyzer;
import net.uaprom.jmorphy2.BaseTestCase;


@RunWith(JUnit4.class)
public class SimpleParserTest extends BaseTestCase {
    private SimpleTagger tagger;
    private SimpleParser parser;

    public SimpleParserTest() throws IOException {
        tagger = new SimpleTagger(analyzer);
        parser = new SimpleParser(analyzer);
    }

    // @Test
    public void testTagger() throws IOException {
        assertTaggedSent(new String[]{
                "(TOP (ADJF,nomn,plur женские) (NOUN,inan,masc,nomn,plur сапоги))",
                "(TOP (ADJF,accs,inan,plur женские) (NOUN,inan,masc,nomn,plur сапоги))",
                "(TOP (ADJF,nomn,plur женские) (NOUN,accs,inan,masc,plur сапоги))",
                "(TOP (ADJF,accs,inan,plur женские) (NOUN,accs,inan,masc,plur сапоги))"},
            tagger.tagAll(new String[]{"женские", "сапоги"}));
        System.out.println(tagger.tagAll(new String[]{"женские", "сапоги", "на", "зиму"}));
        System.out.println(tagger.tag(new String[]{"чехол", "для", "iphone", "4s"}));
        System.out.println(tagger.tag(new String[]{"шуруповерт", "Bosch"}));
        System.out.println("=======================");

        // for (Node top : tagger.tagAll(new String[]{"уборка", "и", "вывоз", "снега", "и", "льда"})) {
        //     System.out.println(top);
        // }
        // System.out.println("=======================");
    }

    private void assertTaggedSent(String[] expected, List<Node.Top> sents) {
        assertEquals(expected.length, sents.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], sents.get(i).toString());
        }
    }

    @Test
    public void testExtractSubject() throws IOException {
        List<Set<String>> enableExtractionValues = Lists.newArrayList();
        enableExtractionValues.add(ImmutableSet.of("NP", "nomn"));
        enableExtractionValues.add(ImmutableSet.of("NP", "accs"));

        List<Set<String>> disableExtractionValues = Lists.newArrayList();
        disableExtractionValues.add(ImmutableSet.of("PP"));

        List<Set<String>> subjValues = Lists.newArrayList();
        subjValues.add(ImmutableSet.of("NOUN", "nomn"));
        subjValues.add(ImmutableSet.of("NOUN", "accs"));
        subjValues.add(ImmutableSet.of("LATN"));
        subjValues.add(ImmutableSet.of("NUMB"));

        SubjectExtractor subjExtractor =
            new SubjectExtractor(enableExtractionValues,
                                 disableExtractionValues,
                                 subjValues,
                                 true);

        Node.Top sent;
        sent = parser.parse(tagger.tagAll(new String[]{"женские", "сапоги"}));
        assertEquals(Arrays.asList(new String[]{"сапог"}),
                     subjExtractor.extract(sent));

        sent = parser.parse(tagger.tagAll(new String[]{"чехол", "ozaki", "для", "iphone", "5"}));
        assertEquals(Arrays.asList(new String[]{"чехол", "ozaki"}),
                     subjExtractor.extract(sent));

        sent = parser.parse(tagger.tagAll(new String[]{"GLOBAL", "Зарядное", "устройство"}));
        assertEquals(Arrays.asList(new String[]{"GLOBAL", "устройство"}),
                     subjExtractor.extract(sent));

        sent = parser.parse(tagger.tagAll(new String[]{"Лыжные", "зимние", "теплые", "перчатки", "HEAD"}));
        assertEquals(Arrays.asList(new String[]{"перчатка", "HEAD"}),
                     subjExtractor.extract(sent));

        sent = parser.parse(tagger.tagAll(new String[]{"Купить", "туристическую", "палатку", "в", "Украине", "VAUDE", "Opera", "4P", "2013", "цвет", "sand"}));
        assertEquals(Arrays.asList(new String[]{"палатка", "VAUDE", "Opera", "4P", "2013", "цвет", "sand"}),
                     subjExtractor.extract(sent));
    }

    @Test
    public void testParser() throws IOException {
        assertEquals("(TOP " +
                       "(NP,nomn,plur " +
                         "(ADJF,nomn,plur женские) " +
                         "(NP,nomn,plur (NOUN,inan,masc,nomn,plur сапоги))" +
                       ")" +
                     ")",
                     parser.parse(tagger.tagAll(new String[]{"женские", "сапоги"})).toString());

        assertEquals("(TOP " +
                       "(NP,nomn,plur " +
                         "(NP,nomn,plur " +
                           "(ADJF,nomn,plur женские) " +
                           "(NP,nomn,plur (NOUN,inan,masc,nomn,plur сапоги))" +
                         ") " +
                         "(ADJF,Qual,nomn,plur коричневые)" +
                       ")" +
                     ")",
                     parser.parse(tagger.tagAll(new String[]{"женские", "сапоги", "коричневые"})).toString());

        assertEquals("(TOP " +
                       "(NP " +
                         "(NP,nomn,sing (NOUN,inan,masc,nomn,sing чехол)) " +
                         "(PP (PREP для) (NP,gent,sing (NOUN,gent,inan,masc,sing телефона)))" +
                       ")" +
                     ")",
                     parser.parse(tagger.tagAll(new String[]{"чехол", "для", "телефона"})).toString());

        assertEquals("(TOP " +
                       "(NP " +
                         "(NP,nomn,sing (NOUN,inan,masc,nomn,sing чехол)) " +
                         "(PP " +
                           "(PREP для) " +
                           "(NP,nomn,sing " +
                             "(NP,nomn,sing (LATN iphone)) " +
                             "(NUMB,intg 5)" +
                            ")" +
                         ")" +
                       ")" +
                     ")",
                     parser.parse(tagger.tagAll(new String[]{"чехол", "для", "iphone", "5"})).toString());

        assertEquals("(TOP (NP,nomn,sing (NP,nomn,sing (LATN iphone)) (NUMB,intg 5)))",
                     parser.parse(tagger.tagAll(new String[]{"iphone", "5"})).toString());

        assertEquals("(TOP " +
                       "(NP,sing " +
                         "(NP,nomn,sing " +
                           "(NP,nomn,sing (NOUN,femn,inan,nomn,sing уборка)) " +
                           "(CONJ и) " +
                           "(NP,nomn,sing (NOUN,inan,masc,nomn,sing вывоз))" +
                         ") " +
                         "(NP,gent,sing " +
                           "(NP,gent,sing (NOUN,gent,inan,masc,sing снега)) " +
                           "(CONJ и) " +
                           "(NP,gent,sing (NOUN,gent,inan,masc,sing льда))" +
                         ")" +
                       ")" +
                     ")",
                     parser.parse(tagger.tagAll(new String[]{"уборка", "и", "вывоз", "снега", "и", "льда"})).toString());
        
        assertEquals("(TOP " +
                       "(NP,sing " +
                         "(NP,sing " +
                           "(NP,nomn,sing (NOUN,femn,inan,nomn,sing уборка)) " +
                           "(NP,gent,sing (NOUN,gent,inan,masc,sing снега))" +
                         ") " +
                         "(CONJ и) " +
                         "(NP,sing " +
                           "(NP,nomn,sing (NOUN,inan,masc,nomn,sing вывоз)) " +
                           "(NP,gent,sing (NOUN,gent,inan,masc,sing льда))" +
                         ")" +
                       ")" +
                     ")",
                     parser.parse(tagger.tagAll(new String[]{"уборка", "снега", "и", "вывоз", "льда"})).toString());

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
        for (int i = 0; i < N * 10; i++) {
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
