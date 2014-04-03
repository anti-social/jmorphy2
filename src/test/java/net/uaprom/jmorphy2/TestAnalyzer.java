package net.uaprom.jmorphy2;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;


@RunWith(JUnit4.class)
public class TestAnalyzer {
    private MorphAnalyzer analyzer;

    public TestAnalyzer() throws IOException {
        Map<Character,String> replaceChars = new HashMap<Character,String>();
        replaceChars.put('е', "ё");
        analyzer = new MorphAnalyzer(replaceChars);
    }

    @Test
    public void test() throws IOException {
        List<Parsed> parseds = analyzer.parse("красивого");
        Tag tag = parseds.get(0).tag;
        assertParseds("красивого:ADJF,Qual neut,sing,gent:красивый:красивого:0.5\n" +
                      "красивого:ADJF,Qual masc,sing,gent:красивый:красивого:0.25\n" +
                      "красивого:ADJF,Qual anim,masc,sing,accs:красивый:красивого:0.25",
                      parseds);
        assertEquals(analyzer.getGrammeme("POST"), analyzer.getGrammeme("ADJF").getRoot());
        assertEquals(analyzer.getGrammeme("ADJF"), tag.POS);
        assertEquals(analyzer.getGrammeme("gent"), tag.Case);
        assertEquals(analyzer.getGrammeme("sing"), tag.number);
        assertEquals(analyzer.getGrammeme("neut"), tag.gender);
        assertTrue(tag.contains("ADJF"));
        assertTrue(tag.containsAllValues(Arrays.asList("ADJF", "gent")));
        assertFalse(tag.contains("NOUN"));
        assertFalse(tag.containsAllValues(Arrays.asList("ADJF", "nomn")));

        // word with unknown prefix
        assertParseds("лошарики:NOUN,inan,masc plur,nomn:лошарик:шарики:0.2\n" +
                      "лошарики:NOUN,inan,masc plur,accs:лошарик:шарики:0.2\n" +
                      "лошарики:NOUN,anim,masc,Name plur,nomn:лошарик:арики:0.2\n" +
                      "лошарики:NOUN,anim,femn,Name sing,gent:лошарика:арики:0.2\n" +
                      "лошарики:NOUN,anim,femn,Name plur,nomn:лошарика:арики:0.2\n",
                      analyzer.parse("лошарики"));

        // unknown word
        assertParseds("псевдокошка:NOUN,anim,femn sing,nomn:псевдокошка:кошка:0.7999999\n" +
                      "псевдокошка:NOUN,inan,femn sing,nomn:псевдокошка:кошка:0.1999999",
                      analyzer.parse("псевдокошка"));

        assertParseds("снега:NOUN,inan,masc sing,gent:снег:снега:0.777777\n" +
                      "снега:NOUN,inan,masc plur,nomn:снег:снега:0.111111\n" +
                      "снега:NOUN,inan,masc plur,accs:снег:снега:0.111111\n",
                      analyzer.parse("снега"));

        // gen2, loct, loc2
        assertParseds("снеге:NOUN,inan,masc sing,loct:снег:снеге:1.0", analyzer.parse("снеге"));
        assertParseds("снегу:NOUN,inan,masc sing,loc2:снег:снегу:0.5\n" +
                      "снегу:NOUN,inan,masc sing,datv:снег:снегу:0.375\n" +
                      "снегу:NOUN,inan,masc sing,gen2:снег:снегу:0.125\n",
                      analyzer.parse("снегу"));

        // е, ё
        assertParseds("ёжик:NOUN,anim,masc sing,nomn:ёжик:ёжик:1.0", analyzer.parse("ёжик"));
        assertParseds("ежик:NOUN,anim,masc sing,nomn:ёжик:ежик:1.0", analyzer.parse("ежик"));

        // NUMB
        assertParseds("1:NUMB,intg:1:1:1.0", analyzer.parse("1"));
        assertParseds("1.0:NUMB,real:1.0:1.0:1.0", analyzer.parse("1.0"));

        // PNCT
        assertParseds(".:PNCT:.:.:1.0", analyzer.parse("."));
        assertParseds(",:PNCT:,:,:1.0", analyzer.parse(","));
        assertParseds("!?:PNCT:!?:!?:1.0", analyzer.parse("!?"));

        // LATN
        assertParseds("test:LATN:test:test:1.0", analyzer.parse("test"));
        assertParseds("test1:LATN:test1:test1:1.0", analyzer.parse("test1"));
        assertParseds("test1.0:LATN:test1.0:test1.0:1.0", analyzer.parse("test1.0"));
        assertParseds(".test.:LATN:.test.:.test.:1.0", analyzer.parse(".test."));
        assertParseds("männer:LATN:männer:männer:1.0", analyzer.parse("männer"));
        assertParseds("", analyzer.parse("тестsymbolmix"));

        // ROMN (all roman numbers are also latin)
        assertParseds("MD:ROMN:MD:MD:0.5\n" +
                      "MD:LATN:MD:MD:0.5\n",
                      analyzer.parse("MD"));

        // TODO: Hyphen

        // normal form
        assertEquals(Arrays.asList("красивый"), analyzer.normalForms("красивого"));
        assertEquals(Arrays.asList("для", "длить"), analyzer.normalForms("для"));
        assertEquals(Arrays.asList("лошарик", "лошарика"), analyzer.normalForms("лошарикам"));

        // tag
        assertEquals(Arrays.asList(analyzer.getTag("ADJF,Qual neut,sing,gent"),
                                   analyzer.getTag("ADJF,Qual masc,sing,gent"),
                                   analyzer.getTag("ADJF,Qual anim,masc,sing,accs")),
                     analyzer.tag("красивого"));
    }

    private void assertParseds(String expectedString, List<Parsed> parseds) throws IOException {
        List<Parsed> expected = new ArrayList<Parsed>();
        for (String s : expectedString.split("\n")) {
            if (s.equals("")) {
                continue;
            }
            String[] parts = s.split(":");
            expected.add(new Parsed(parts[0],
                                    analyzer.getTag(parts[1]),
                                    parts[2],
                                    parts[3],
                                    Float.parseFloat(parts[4])));
        }
        assertEquals(expected, parseds);
    }
}
