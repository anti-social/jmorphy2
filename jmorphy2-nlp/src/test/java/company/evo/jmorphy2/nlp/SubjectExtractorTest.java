package company.evo.jmorphy2.nlp;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.Jmorphy2TestsHelpers;


@RunWith(JUnit4.class)
public class SubjectExtractorTest {
    private MorphAnalyzer morph;
    protected SubjectExtractor subjExtractor;
    private boolean initialized = false;

    @Before
    public void setUp() throws IOException {
        if (initialized) {
            return;
        }
        morph = Jmorphy2TestsHelpers.newMorphAnalyzer("/pymorphy2_dicts_ru");
        Parser parser = new SimpleParser(morph, new SimpleTagger(morph), 100);
        subjExtractor =
            new SubjectExtractor(parser,
                                 "+NP,nomn +NP,accs -PP -Geox NOUN,nomn NOUN,accs LATN NUMB",
                                 true);
        initialized = true;
    }

    @Test
    public void testExtractSubject() throws IOException {
        assertEquals(Arrays.asList("игрушка", "Lava", "кукла", "майя"),
                     subjExtractor.extract("мягкая муз игрушка Lava кукла майя в красном платье".split(" ")));

        assertEquals(Arrays.asList(new String[]{"сапог"}),
                     subjExtractor.extract(new String[]{"женские", "сапоги"}));

        assertEquals(Arrays.asList(new String[]{"сапог"}),
                     subjExtractor.extract(new String[]{"женские", "сапоги", "днепропетровск"}));

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
