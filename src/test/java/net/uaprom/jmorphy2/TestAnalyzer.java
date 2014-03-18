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
    @Test
    public void test() throws IOException {
        Map<Character,String> replaceChars = new HashMap<Character,String>();
        replaceChars.put('е', "ё");
        MorphAnalyzer analyzer = new MorphAnalyzer(replaceChars);

        List<Parsed> parseds = analyzer.parse("красивого");
        Tag tag = parseds.get(0).tag;
        assertParseds("красивого:ADJF,Qual masc,sing,gent:красивый:1.0\n"
                      + "красивого:ADJF,Qual anim,masc,sing,accs:красивый:1.0\n"
                      + "красивого:ADJF,Qual neut,sing,gent:красивый:1.0",
                      parseds);
        assertEquals("ADJF", tag.POS);
        assertEquals("gent", tag.Case);
        assertEquals("sing", tag.number);
        assertEquals("masc", tag.gender);
        assertTrue(tag.contains("ADJF"));
        assertTrue(tag.containsAll(Arrays.asList("ADJF", "gent")));
        assertFalse(tag.contains("NOUN"));
        assertFalse(tag.containsAll(Arrays.asList("ADJF", "nomn")));

        assertParseds("ёжик:NOUN,anim,masc sing,nomn:ёжик:1.0", analyzer.parse("ёжик"));
        assertParseds("ежик:NOUN,anim,masc sing,nomn:ёжик:1.0", analyzer.parse("ежик"));

        assertEquals(Arrays.asList("красивый"), analyzer.getNormalForms("красивого"));
        assertEquals(Arrays.asList("для", "длить"), analyzer.getNormalForms("для"));

        // MorphAnalyzer analyzer2 = new MorphAnalyzer();
    }

    private void assertParseds(String expectedString, List<Parsed> parseds) throws IOException {
        List<Parsed> expected = new ArrayList<Parsed>();
        for (String s : expectedString.split("\n")) {
            String[] parts = s.split(":");
            expected.add(new Parsed(parts[0], new Tag(parts[1]), parts[2], Float.parseFloat(parts[3])));
        }
        assertEquals(expected, parseds);
    }

    // @Test(expected=IllegalArgumentException.class)
    // public void testDummyKey() {
    //     testHashTable(new int[] {Integer.MIN_VALUE}, new float[] {1.1f}, 8);
    // }

    // @Test(expected=IllegalArgumentException.class)
    // public void testDummyKey2() {
    //     testHashTable(new int[] {Integer.MIN_VALUE, 0, 1}, new float[] {-1.1f, 0.0f, 1.1f}, 36);
    // }
}
