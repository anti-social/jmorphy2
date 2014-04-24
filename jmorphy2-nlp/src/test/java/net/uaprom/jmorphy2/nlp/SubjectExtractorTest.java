package net.uaprom.jmorphy2.nlp;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import net.uaprom.jmorphy2.test._BaseTestCase;


@RunWith(JUnit4.class)
public class SubjectExtractorTest extends _BaseTestCase {
    private SubjectExtractor subjExtractor;
    private boolean initialized = false;

    @Before
    public void setUp() throws IOException {
        if (initialized) {
            return;
        }
        initMorphAnalyzer();
        Parser parser = new SimpleParser(morph, new SimpleTagger(morph));
        subjExtractor =
            new SubjectExtractor(parser,
                                 "+NP,nomn +NP,accs -PP NOUN,nomn NOUN,accs LATN NUMB",
                                 true);
        initialized = true;
    }

    @Test
    public void testExtractSubject() throws IOException {
        assertEquals(Arrays.asList(new String[]{"сапог"}),
                     subjExtractor.extract(new String[]{"женские", "сапоги"}));

        assertEquals(Arrays.asList(new String[]{"чехол", "ozaki"}),
                     subjExtractor.extract(new String[]{"чехол", "ozaki", "для", "iphone", "5"}));

        assertEquals(Arrays.asList(new String[]{"магнит"}),
                     subjExtractor.extract(new String[]{"магнит", "на", "холодильник"}));

        assertEquals(Arrays.asList(new String[]{"GLOBAL", "устройство"}),
                     subjExtractor.extract(new String[]{"GLOBAL", "Зарядное", "устройство"}));

        assertEquals(Arrays.asList(new String[]{"перчатка", "HEAD"}),
                     subjExtractor.extract(new String[]{"Лыжные", "зимние", "теплые", "перчатки", "HEAD"}));

        assertEquals(Arrays.asList(new String[]{"палатка", "цвет", "sand"}),
                     subjExtractor.extract(new String[]{"Купить", "туристическую", "палатку", "в", "Украине", "VAUDE", "Opera", "4P", "2013", "цвет", "sand"}));
    }
}
