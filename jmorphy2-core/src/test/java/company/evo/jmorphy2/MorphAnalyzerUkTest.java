package company.evo.jmorphy2;

import org.junit.Test;
import org.junit.Before;
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
public class MorphAnalyzerUkTest {
    private MorphAnalyzer morph;
    private boolean initialized = false;

    @Before
    public void setUp() throws IOException {
        if (!initialized) {
            morph = Jmorphy2TestsHelpers.newMorphAnalyzer("uk");
            initialized = true;
        }
    }

    @Test
    public void test_parse() throws IOException {
        List<ParsedWord> parseds;
        Tag tag;

        parseds = morph.parse("чарівної");
        tag = parseds.get(0).tag;
        assertParseds("чарівної:ADJF,compb femn,gent:чарівний:чарівної:1.0\n",
                      parseds);
        assertEquals(morph.getGrammeme("ADJF"), tag.POS);
        assertEquals(morph.getGrammeme("gent"), tag.Case);
        assertEquals(morph.getGrammeme("femn"), tag.gender);
        assertTrue(tag.contains("ADJF"));
        assertTrue(tag.containsAllValues(Arrays.asList("ADJF", "gent")));

        parseds = morph.parse("комп'ютер");
        assertParseds("комп'ютер:NOUN,inan masc,nomn:комп'ютер:комп'ютер:1.0\n" +
                      "комп'ютер:NOUN,inan masc,accs:комп'ютер:комп'ютер:1.0\n",
                      parseds);
        parseds = morph.parse("комп\u2019ютер");
        assertParseds("комп\u2019ютер:NOUN,inan masc,nomn:комп'ютер:комп'ютер:1.0\n" +
                      "комп\u2019ютер:NOUN,inan masc,accs:комп'ютер:комп'ютер:1.0\n",
                      parseds);
        parseds = morph.parse("комп\u02bcютер");
        assertParseds("комп\u02bcютер:NOUN,inan masc,nomn:комп'ютер:комп'ютер:1.0\n" +
                      "комп\u02bcютер:NOUN,inan masc,accs:комп'ютер:комп'ютер:1.0\n",
                      parseds);

        parseds = morph.parse("3D-графіка");
        assertParseds("3d-графіка:NOUN,inan masc,gent:3d-графік:графіка:0.75\n" +
                      "3d-графіка:NOUN,anim masc,gent:3d-графік:графіка:0.75\n" +
                      "3d-графіка:NOUN,anim masc,accs:3d-графік:графіка:0.75\n" +
                      "3d-графіка:NOUN,inan femn,nomn:3d-графіка:графіка:0.75\n",
                      parseds);

        parseds = morph.parse("сьогодні");
        assertParseds("сьогодні:ADVB:сьогодні:сьогодні:1.0", parseds, true);
    }

    @Test
    public void test_getLexeme() throws IOException {
        List<ParsedWord> parseds;
        List<ParsedWord> lexeme;

        parseds = morph.parse("чарівної");
        lexeme = parseds.get(0).getLexeme();
        assertParseds("чарівний:ADJF,compb masc,nomn:чарівний:чарівний:1.0\n" +
                      "чарівного:ADJF,compb masc,gent:чарівний:чарівного:1.0\n" +
                      "чарівному:ADJF,compb masc,datv:чарівний:чарівному:1.0\n" +
                      "чарівного:ADJF,compb masc,accs:чарівний:чарівного:1.0\n" +
                      "чарівний:ADJF,compb masc,accs:чарівний:чарівний:1.0\n" +
                      "чарівним:ADJF,compb masc,ablt:чарівний:чарівним:1.0\n" +
                      "чарівнім:ADJF,compb masc,loct:чарівний:чарівнім:1.0\n" +
                      "чарівному:ADJF,compb masc,loct:чарівний:чарівному:1.0\n" +
                      "чарівний:ADJF,compb masc,voct:чарівний:чарівний:1.0\n" +
                      "чарівна:ADJF,compb femn,nomn:чарівний:чарівна:1.0\n",
                      lexeme,
                      false);
        assertEquals(32, lexeme.size());

        // known prefix
        parseds = morph.parse("авіаквиток");
        lexeme = parseds.get(0).getLexeme();
        assertParseds("авіаквиток:NOUN,inan masc,nomn:авіаквиток:квиток:1.0\n" +
                      "авіаквитка:NOUN,inan masc,gent:авіаквиток:квитка:1.0",
                      lexeme,
                      false);
        assertEquals(16, lexeme.size());

        // Unknown word
        assertParseds("їъ:UNKN:їъ:їъ:1.0", morph.parse("їъ").get(0).getLexeme());
    }

    @Test
    public void test_inflect() throws IOException {
        List<ParsedWord> parseds;
        List<ParsedWord> paradigm;

        parseds = morph.parse("чарівної");
        paradigm = parseds.get(0).inflect(Arrays.asList(morph.getGrammeme("ADJF"),
                                                        morph.getGrammeme("plur")),
                                          Arrays.asList(morph.getGrammeme("Supr")));
        assertParseds("чарівні:ADJF,compb plur,nomn:чарівний:чарівні:1.0\n" +
                      "чарівних:ADJF,compb plur,gent:чарівний:чарівних:1.0\n" +
                      "чарівним:ADJF,compb plur,datv:чарівний:чарівним:1.0\n" +
                      "чарівних:ADJF,compb plur,accs:чарівний:чарівних:1.0\n" +
                      "чарівні:ADJF,compb plur,accs:чарівний:чарівні:1.0\n" +
                      "чарівними:ADJF,compb plur,ablt:чарівний:чарівними:1.0\n" +
                      "чарівних:ADJF,compb plur,loct:чарівний:чарівних:1.0\n" +
                      "чарівні:ADJF,compb plur,voct:чарівний:чарівні:1.0",
                      paradigm);
    }

    private void assertParseds(String expectedString, List<ParsedWord> parseds) throws IOException {
        assertParseds(expectedString, parseds, true);
    }

    private void assertParseds(String expectedString, List<ParsedWord> parseds, boolean checkLength) throws IOException {
        List<ParsedWordMock> expected = new ArrayList<>();
        for (String s : expectedString.split("\n")) {
            if (s.equals("")) {
                continue;
            }
            String[] parts = s.split(":");
            expected.add(new ParsedWordMock(parts[0],
                                            morph.getTag(parts[1]), 
                                            parts[2],
                                            parts[3],
                                            Float.parseFloat(parts[4])));
        }
        if (!checkLength) {
            assertEquals(expected, parseds.subList(0, expected.size()));
        } else {
            assertEquals(expected, parseds);
        }
    }

    class ParsedWordMock extends ParsedWord {
        public ParsedWordMock(String word, Tag tag, String normalForm, String foundWord, float score) {
            super(word, tag, normalForm, foundWord, score);
        }

        @Override
        public ParsedWord rescore(float newScore) {
            return new ParsedWordMock(word, tag, normalForm, foundWord, newScore);
        }

        @Override
        public List<ParsedWord> getLexeme() {
            return new ArrayList<>();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ParsedWord) {
                ParsedWord other = (ParsedWord) obj;
                return word.equals(other.word)
                    && tag.equals(other.tag)
                    && normalForm.equals(other.normalForm)
                    && Math.abs(score - other.score) < EPS;
            }
            return false;
        }
    }
}
