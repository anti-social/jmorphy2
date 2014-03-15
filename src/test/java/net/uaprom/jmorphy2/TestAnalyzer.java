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
