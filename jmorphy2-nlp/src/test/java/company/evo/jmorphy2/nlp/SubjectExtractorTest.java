package company.evo.jmorphy2.nlp;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.Jmorphy2TestsHelpers;


@RunWith(JUnit4.class)
public class SubjectExtractorTest {
    protected SubjectExtractor subjExtractor;
    private boolean initialized = false;

    @Before
    public void setUp() throws IOException {
        if (initialized) {
            return;
        }
        MorphAnalyzer morph = Jmorphy2TestsHelpers.newMorphAnalyzer("ru");
        Parser parser = new SimpleParser(morph, new SimpleTagger(morph), 100);
        subjExtractor =
            new SubjectExtractor(parser,
                                 "+NP,nomn +NP,accs -PP -Geox NOUN,nomn NOUN,accs LATN NUMB",
                                 true);
        initialized = true;
    }

    @Test
    public void testExtractSubject() throws IOException {
        assertEquals(List.of("игрушка", "Lava", "кукла", "майя"),
                     subjExtractor.extract("мягкая муз игрушка Lava кукла майя в красном платье".split(" ")));

        assertEquals(List.of("сапог"),
                     subjExtractor.extract(new String[]{"женские", "сапоги"}));

        assertEquals(List.of("сапог"),
                     subjExtractor.extract(new String[]{"женские", "сапоги", "днепропетровск"}));

        assertEquals(List.of("чехол", "ozaki"),
                     subjExtractor.extract(new String[]{"чехол", "ozaki", "для", "iphone", "5"}));

        assertEquals(List.of("магнит"),
                     subjExtractor.extract(new String[]{"магнит", "на", "холодильник"}));

        assertEquals(List.of("GLOBAL", "устройство"),
                     subjExtractor.extract(new String[]{"GLOBAL", "Зарядное", "устройство"}));

        assertEquals(List.of("перчатка", "HEAD"),
                     subjExtractor.extract(new String[]{"Лыжные", "зимние", "теплые", "перчатки", "HEAD"}));

        assertEquals(List.of("палатка", "цвет", "sand"),
                     subjExtractor.extract(new String[]{"Купить", "туристическую", "палатку", "в", "Украине", "VAUDE", "Opera", "4P", "2013", "цвет", "sand"}));
    }
}
