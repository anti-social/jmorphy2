package net.uaprom.jmorphy2.nlp;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import net.uaprom.jmorphy2.test._BaseTestCase;


@RunWith(JUnit4.class)
public class SimpleTaggerTest extends _BaseTestCase {
    private SimpleTagger tagger;
    private boolean initialized = false;

    private static final String TAGGER_RULES_RESOURCE = "/tagger_rules.txt";

    @Before
    public void setUp() throws IOException {
        if (initialized) {
            return;
        }
        initMorphAnalyzer();
        tagger = new SimpleTagger(morph,
                                  new Ruleset(getClass().getResourceAsStream(TAGGER_RULES_RESOURCE)));
        initialized = true;
    }

    @Test
    public void testTagger() throws IOException {
        assertSents(Lists.newArrayList("(TOP (ADJF,nomn,plur женские) (NOUN,inan,masc,nomn,plur сапоги))",
                                       "(TOP (ADJF,accs,inan,plur женские) (NOUN,inan,masc,nomn,plur сапоги))",
                                       "(TOP (ADJF,nomn,plur женские) (NOUN,accs,inan,masc,plur сапоги))",
                                       "(TOP (ADJF,accs,inan,plur женские) (NOUN,accs,inan,masc,plur сапоги))"),
                    tagger.tagAll(new String[]{"женские", "сапоги"}));
        assertSents(Lists.newArrayList("(TOP (ADJF,nomn,plur женские) (NOUN,inan,masc,nomn,plur сапоги) (PREP на) (NOUN,accs,femn,inan,sing зиму))",
                                       "(TOP (ADJF,accs,inan,plur женские) (NOUN,inan,masc,nomn,plur сапоги) (PREP на) (NOUN,accs,femn,inan,sing зиму))",
                                       "(TOP (ADJF,nomn,plur женские) (NOUN,accs,inan,masc,plur сапоги) (PREP на) (NOUN,accs,femn,inan,sing зиму))",
                                       "(TOP (ADJF,accs,inan,plur женские) (NOUN,accs,inan,masc,plur сапоги) (PREP на) (NOUN,accs,femn,inan,sing зиму))"),
                    tagger.tagAll(new String[]{"женские", "сапоги", "на", "зиму"}));
        assertSents(Lists.newArrayList("(TOP (NOUN,inan,masc,nomn,sing чехол) (PREP для) (LATN iphone) (LATN 4s))",
                                       "(TOP (NOUN,accs,inan,masc,sing чехол) (PREP для) (LATN iphone) (LATN 4s))"),
                    tagger.tagAll(new String[]{"чехол", "для", "iphone", "4s"}));
        assertSents(Lists.newArrayList("(TOP (UNKN шуруповерт) (LATN Bosch))"),
                    tagger.tagAll(new String[]{"шуруповерт", "Bosch"}));
    }

    private void assertSents(List<String> expected, List<Node.Top> sents) {
        List<String> stringSents = Lists.newArrayList();
        for (Node sent : sents) {
            stringSents.add(sent.toString());
        }
        assertEquals(expected, stringSents);
    }
}
