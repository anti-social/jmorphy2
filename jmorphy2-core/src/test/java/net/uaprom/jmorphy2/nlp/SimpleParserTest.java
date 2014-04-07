package net.uaprom.jmorphy2.nlp;

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
import net.uaprom.jmorphy2.BaseTestCase;


@RunWith(JUnit4.class)
public class SimpleParserTest extends BaseTestCase {
    private SimpleTagger tagger;
    private SimpleParser parser;

    public SimpleParserTest() throws IOException {
        tagger = new SimpleTagger(analyzer);
        parser = new SimpleParser(analyzer, tagger);
    }

    @Test
    public void testParse() throws IOException {
        System.out.println(parser.topParse(new String[]{"женские", "сапоги"}));
        System.out.println(parser.topParse(new String[]{"чехол", "для", "телефона"}));
        System.out.println(parser.topParse(new String[]{"чехол", "для", "iphone", "5"}));
        // System.out.println(parser.topParse(new String[]{"женские", "сапоги", "коричневые"}));
        // System.out.println(parser.topParse(new String[]{"уборка", "и", "вывоз", "снега", "и", "льда"}));
        // System.out.println(parser.topParse(new String[]{"уборка", "снега", "и", "вывоз", "льда"}));
    }
}
