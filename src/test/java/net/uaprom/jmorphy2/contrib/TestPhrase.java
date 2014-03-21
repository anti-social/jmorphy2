package net.uaprom.jmorphy2.contrib;

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

import net.uaprom.jmorphy2.MorphAnalyzer;


@RunWith(JUnit4.class)
public class TestPhrase {
    private MorphAnalyzer analyzer;

    public TestPhrase() throws IOException {
        Map<Character,String> replaceChars = new HashMap<Character,String>();
        replaceChars.put('е', "ё");
        analyzer = new MorphAnalyzer(replaceChars);
    }

    @Test
    public void testParse() throws IOException {
        Phrase phrase = new Phrase("женские сапоги", analyzer);
        // assertEquals(Arrays.asList(new Phrase.Word("женские", Phrase.Grammar.SUBJECT),
        //                            new Phrase.Word("сапоги", Phrase.Grammar.SUBJECT)),
        //              phrase.parsedWords);
    }
}
