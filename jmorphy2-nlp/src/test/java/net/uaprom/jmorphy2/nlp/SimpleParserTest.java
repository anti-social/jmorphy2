package net.uaprom.jmorphy2.nlp;

import org.junit.Test;
import org.junit.Before;
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

import net.uaprom.jmorphy2.test._BaseTestCase;


@RunWith(JUnit4.class)
public class SimpleParserTest extends _BaseTestCase {
    private SimpleTagger tagger;
    private SimpleParser parser;

    @Before
    public void setUp() throws IOException {
        initAnalyzer();
        tagger = new SimpleTagger(analyzer);
        parser = new SimpleParser(analyzer);
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
    }
}
