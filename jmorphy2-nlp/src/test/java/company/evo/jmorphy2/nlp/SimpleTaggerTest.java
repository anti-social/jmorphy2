package company.evo.jmorphy2.nlp;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.Jmorphy2TestsHelpers;


@RunWith(JUnit4.class)
public class SimpleTaggerTest {
    private MorphAnalyzer morph;
    private SimpleTagger tagger;
    private boolean initialized = false;

    private static final String TAGGER_RULES_RESOURCE = "/tagger_rules.txt";

    @Before
    public void setUp() throws IOException {
        if (initialized) {
            return;
        }
        morph = Jmorphy2TestsHelpers.newMorphAnalyzer("ru");
        tagger = new SimpleTagger(morph,
                                  new Ruleset(getClass().getResourceAsStream(TAGGER_RULES_RESOURCE)));
        initialized = true;
    }

    @Test
    public void testTagger() throws IOException {
        assertSents(
            new ArrayList<String>() {{
                add("(TOP (ADJF,accs,inan,plur женские) (NOUN,inan,masc,nomn,plur сапоги))");
                add("(TOP (ADJF,accs,inan,plur женские) (NOUN,accs,inan,masc,plur сапоги))");
                add("(TOP (ADJF,nomn,plur женские) (NOUN,inan,masc,nomn,plur сапоги))");
                add("(TOP (ADJF,nomn,plur женские) (NOUN,accs,inan,masc,plur сапоги))");
            }},
            tagger.tagAll(new String[]{"женские", "сапоги"})
        );
        assertSents(
            new ArrayList<String>() {{
                add("(TOP (ADJF,accs,inan,plur женские) (NOUN,inan,masc,nomn,plur сапоги) (PREP на) (NOUN,accs,femn,inan,sing зиму))");
                add("(TOP (ADJF,accs,inan,plur женские) (NOUN,accs,inan,masc,plur сапоги) (PREP на) (NOUN,accs,femn,inan,sing зиму))");
                add("(TOP (ADJF,nomn,plur женские) (NOUN,inan,masc,nomn,plur сапоги) (PREP на) (NOUN,accs,femn,inan,sing зиму))");
                add("(TOP (ADJF,nomn,plur женские) (NOUN,accs,inan,masc,plur сапоги) (PREP на) (NOUN,accs,femn,inan,sing зиму))");
            }},
            tagger.tagAll(new String[]{"женские", "сапоги", "на", "зиму"})
        );
        assertSents(
            new ArrayList<String>() {{
                add("(TOP (NOUN,inan,masc,nomn,sing чехол) (PREP для) (LATN iphone) (LATN 4s))");
                add("(TOP (NOUN,accs,inan,masc,sing чехол) (PREP для) (LATN iphone) (LATN 4s))");
            }},
            tagger.tagAll(new String[]{"чехол", "для", "iphone", "4s"}));
//         FIXME
//         assertSents(
//             new ArrayList<String>() {{
//                 add("(TOP (NOUN,anim,masc,nomn,sing шуруповерт) (LATN Bosch))");
//             }},
//             tagger.tagAll(new String[]{"шуруповерт", "Bosch"})
//         );
    }

    private void assertSents(List<String> expected, List<Node.Top> sents) {
        List<String> stringSents = new ArrayList<String>();
        for (Node sent : sents) {
            stringSents.add(sent.toString());
        }
        assertEquals(expected, stringSents);
    }
}
