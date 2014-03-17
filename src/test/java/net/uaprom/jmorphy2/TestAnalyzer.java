package net.uaprom.jmorphy2;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;


@RunWith(JUnit4.class)
public class TestAnalyzer {
    @Test
    public void test() throws IOException {
        MorphAnalyzer analyzer = new MorphAnalyzer();
        List<Parsed> expected =
            Arrays.asList(new Parsed("красивого", new Tag("ADJF,Qual masc,sing,gent"), "красивый", 1.0f),
                          new Parsed("красивого", new Tag("ADJF,Qual anim,masc,sing,accs"), "красивый", 1.0f),
                          new Parsed("красивого", new Tag("ADJF,Qual neut,sing,gent"), "красивый", 1.0f));
        List<Parsed> parseds = analyzer.parse("красивого");
        Tag tag = parseds.get(0).tag;
        assertEquals(expected, parseds);
        assertEquals("ADJF", tag.POS);
        assertEquals("gent", tag.Case);
        assertEquals("sing", tag.number);
        assertEquals("masc", tag.gender);
        assertTrue(tag.contains("ADJF"));
        assertFalse(tag.contains("NOUN"));

        assertEquals(Arrays.asList("красивый"), analyzer.getNormalForms("красивого"));
        assertEquals(Arrays.asList("для", "длить"), analyzer.getNormalForms("для"));
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
