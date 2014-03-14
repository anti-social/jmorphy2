package net.uaprom.jmorphy2;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;


@RunWith(JUnit4.class)
public class TestAnalyzer {
    @Test
    public void test() throws IOException {
        MorphAnalyzer analyzer = new MorphAnalyzer();
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
