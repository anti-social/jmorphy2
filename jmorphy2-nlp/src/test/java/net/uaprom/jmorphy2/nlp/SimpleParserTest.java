package net.uaprom.jmorphy2.nlp;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import net.uaprom.jmorphy2.test._BaseTestCase;


@RunWith(JUnit4.class)
public class SimpleParserTest extends _BaseTestCase {
    private Parser parser;
    private boolean initialized = false;

    private static final String TAGGER_RULES_RESOURCE = "/tagger_rules.txt";
    private static final String PARSER_RULES_RESOURCE = "/parser_rules.txt";

    @Before
    public void setUp() throws IOException {
        if (initialized) {
            return;
        }
        initMorphAnalyzer();
        parser = new SimpleParser(morph,
                                  new SimpleTagger(morph,
                                                   new Ruleset(getClass().getResourceAsStream(TAGGER_RULES_RESOURCE))),
                                  new Ruleset(getClass().getResourceAsStream(PARSER_RULES_RESOURCE)));
        initialized = true;
    }

    // @Test
    public void test() throws IOException {
        // List<Node.Top> sents = parser.parseAll(new String[]{"чехол", "для", "телефона", "iphone"});
        // List<Node.Top> sents = parser.parseAll(new String[]{"женские", "сапоги"});
        List<Node.Top> sents = parser.parseAll(new String[]{"уборка", "и", "вывоз", "снега", "и", "льда"});
        // List<Node.Top> sents = parser.parseAll(new String[]{"уборка", "снега", "и", "вывоз", "льда"});
        for (Node.Top sent : sents) {
            System.out.println(sent.prettyToString(true));
        }
    }

    @Test
    public void testParser() throws IOException {
        assertEquals("(TOP " +
                       "(NP,nomn,plur " +
                         "(ADJF,nomn,plur женские) " +
                         "(NOUN,inan,masc,nomn,plur сапоги)))",
                     parser.parse(new String[]{"женские", "сапоги"}).toString());

        assertEquals("(TOP " +
                       "(NP,nomn,plur " +
                         "(ADJF,nomn,plur женские) " +
                         "(NOUN,inan,masc,nomn,plur сапоги) " +
                         "(ADJF,Qual,nomn,plur коричневые)))",
                     parser.parse(new String[]{"женские", "сапоги", "коричневые"}).toString());

        assertEquals("(TOP " +
                       "(NP " +
                         "(NP,nomn,sing (NOUN,inan,masc,nomn,sing чехол)) " +
                         "(PP " +
                           "(PREP для) " +
                           "(NP,gent,sing (NOUN,gent,inan,masc,sing телефона)))))",
                     parser.parse(new String[]{"чехол", "для", "телефона"}).toString());

        assertEquals("(TOP " +
                       "(NP " +
                         "(NP,nomn,sing " +
                           "(NOUN,inan,masc,nomn,sing чехол)) " +
                         "(PP " +
                           "(PREP для) " +
                           "(NP,gent,sing " +
                             "(NOUN,gent,inan,masc,sing телефона) " +
                             "(LATN iphone)))))",
                     parser.parse(new String[]{"чехол", "для", "телефона", "iphone"}).toString());

        assertEquals("(TOP " +
                       "(NP " +
                         "(NP,nomn,sing (NOUN,inan,masc,nomn,sing чехол)) " +
                         "(PP " +
                           "(PREP для) " +
                           "(NP,nomn,sing " +
                             "(LATN iphone) " +
                             "(NUMB,intg 5)))))",
                     parser.parse(new String[]{"чехол", "для", "iphone", "5"}).toString());

        assertEquals("(TOP " +
                       "(NP,nomn,sing " +
                         "(LATN iphone) " +
                         "(NUMB,intg 5)))",
                     parser.parse(new String[]{"iphone", "5"}).toString());

        assertEquals("(TOP " +
                       "(NP,sing " +
                         "(NP,nomn,sing " +
                           "(NP,nomn,sing " +
                             "(NOUN,femn,inan,nomn,sing уборка)) " +
                           "(CONJ и) " +
                           "(NP,nomn,sing " +
                             "(NOUN,inan,masc,nomn,sing вывоз))) " +
                         "(NP,gent,sing " +
                           "(NP,gent,sing " +
                             "(NOUN,gent,inan,masc,sing снега)) " +
                           "(CONJ и) " +
                           "(NP,gent,sing " +
                             "(NOUN,gent,inan,masc,sing льда)))))",
                     parser.parse(new String[]{"уборка", "и", "вывоз", "снега", "и", "льда"}).toString());
        
        assertEquals("(TOP " +
                       "(NP,sing " +
                         "(NP,sing " +
                           "(NP,nomn,sing (NOUN,femn,inan,nomn,sing уборка)) " +
                           "(NP,gent,sing (NOUN,gent,inan,masc,sing снега))) " +
                         "(CONJ и) " +
                         "(NP,sing " +
                           "(NP,nomn,sing (NOUN,inan,masc,nomn,sing вывоз)) " +
                           "(NP,gent,sing (NOUN,gent,inan,masc,sing льда)))))",
                     parser.parse(new String[]{"уборка", "снега", "и", "вывоз", "льда"}).toString());
    }
}
