package net.uaprom.jmorphy2;

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
public class MorphAnalyzerTest {
    private MorphAnalyzer morph;
    private boolean initialized = false;

    @Before
    public void setUp() throws IOException {
        if (!initialized) {
            morph = Jmorphy2TestsHelpers.newMorphAnalyzer();
        }
    }

    @Test
    public void test_parse() throws IOException {
        List<ParsedWord> parseds;

        parseds = morph.parse("красивого");
        Tag tag = parseds.get(0).tag;
        assertParseds("красивого:ADJF,Qual neut,sing,gent:красивый:красивого:0.5\n" +
                      "красивого:ADJF,Qual masc,sing,gent:красивый:красивого:0.25\n" +
                      "красивого:ADJF,Qual anim,masc,sing,accs:красивый:красивого:0.25",
                      parseds);
        assertEquals(morph.getGrammeme("POST"), morph.getGrammeme("ADJF").getRoot());
        assertEquals(morph.getGrammeme("ADJF"), tag.POS);
        assertEquals(morph.getGrammeme("gent"), tag.Case);
        assertEquals(morph.getGrammeme("sing"), tag.number);
        assertEquals(morph.getGrammeme("neut"), tag.gender);
        assertTrue(tag.contains("ADJF"));
        assertTrue(tag.containsAllValues(Arrays.asList("ADJF", "gent")));
        assertFalse(tag.contains("NOUN"));
        assertFalse(tag.containsAllValues(Arrays.asList("ADJF", "nomn")));

        // word with unknown prefix
        assertParseds("лошарики:NOUN,inan,masc plur,nomn:лошарик:шарики:0.2\n" +
                      "лошарики:NOUN,inan,masc plur,accs:лошарик:шарики:0.2\n" +
                      "лошарики:NOUN,anim,masc,Name plur,nomn:лошарик:арики:0.2\n" +
                      "лошарики:NOUN,anim,femn,Name sing,gent:лошарика:арики:0.2\n" +
                      "лошарики:NOUN,anim,femn,Name plur,nomn:лошарика:арики:0.2\n",
                      morph.parse("лошарики"));

        // unknown word
        assertParseds("псевдокошка:NOUN,anim,femn sing,nomn:псевдокошка:кошка:0.8333333\n" +
                      "псевдокошка:NOUN,inan,femn sing,nomn:псевдокошка:кошка:0.1666666",
                      morph.parse("псевдокошка"));

        assertParseds("снега:NOUN,inan,masc sing,gent:снег:снега:0.818181\n" +
                      "снега:NOUN,inan,masc plur,nomn:снег:снега:0.090909\n" +
                      "снега:NOUN,inan,masc plur,accs:снег:снега:0.090909\n",
                      morph.parse("снега"));

        // gen2, loct, loc2
        assertParseds("снеге:NOUN,inan,masc sing,loct:снег:снеге:1.0", morph.parse("снеге"));
        assertParseds("снегу:NOUN,inan,masc sing,loc2:снег:снегу:0.5\n" +
                      "снегу:NOUN,inan,masc sing,datv:снег:снегу:0.375\n" +
                      "снегу:NOUN,inan,masc sing,gen2:снег:снегу:0.125\n",
                      morph.parse("снегу"));

        // е, ё
        assertParseds("ёжик:NOUN,anim,masc sing,nomn:ёжик:ёжик:1.0", morph.parse("ёжик"));
        assertParseds("ежик:NOUN,anim,masc sing,nomn:ёжик:ёжик:1.0", morph.parse("ежик"));
        assertParseds("теплые:ADJF,Qual plur,nomn:тёплый:тёплые:0.5\n" +
                      "теплые:ADJF,Qual inan,plur,accs:тёплый:тёплые:0.5",
                      morph.parse("теплые"));

        // known prefix
        parseds = morph.parse("лжекот");
        assertParseds("лжекот:NOUN,anim,masc sing,nomn:лжекот:кот:1.0",
                      morph.parse("лжекот"));

        // unknown prefix
        assertParseds("лошарикам:NOUN,inan,masc plur,datv:лошарик:шарикам:0.333333\n" +
                      "лошарикам:NOUN,anim,masc,Name plur,datv:лошарик:арикам:0.333333\n" +
                      "лошарикам:NOUN,anim,femn,Name plur,datv:лошарика:арикам:0.333333",
                      morph.parse("лошарикам"));

        // NUMB
        assertParseds("1:NUMB,intg:1:1:1.0", morph.parse("1"));
        assertParseds("1.0:NUMB,real:1.0:1.0:1.0", morph.parse("1.0"));

        // PNCT
        assertParseds(".:PNCT:.:.:1.0", morph.parse("."));
        assertParseds(",:PNCT:,:,:1.0", morph.parse(","));
        assertParseds("!?:PNCT:!?:!?:1.0", morph.parse("!?"));

        // LATN
        assertParseds("test:LATN:test:test:1.0", morph.parse("test"));
        assertParseds("test1:LATN:test1:test1:1.0", morph.parse("test1"));
        assertParseds("test1.0:LATN:test1.0:test1.0:1.0", morph.parse("test1.0"));
        assertParseds(".test.:LATN:.test.:.test.:1.0", morph.parse(".test."));
        assertParseds("männer:LATN:männer:männer:1.0", morph.parse("männer"));
        assertParseds("", morph.parse("тестsymbolmix"));

        // ROMN (all roman numbers are also latin)
        assertParseds("MD:ROMN:MD:MD:0.5\n" +
                      "MD:LATN:MD:MD:0.5\n",
                      morph.parse("MD"));

        // TODO: Hyphen

        // normal form
        assertEquals(Arrays.asList("красивый"), morph.normalForms("красивого"));
        assertEquals(Arrays.asList("для", "длить"), morph.normalForms("для"));
        assertEquals(Arrays.asList("лошарик", "лошарика"), morph.normalForms("лошарикам"));

        // tag
        assertEquals(Arrays.asList(morph.getTag("ADJF,Qual neut,sing,gent"),
                                   morph.getTag("ADJF,Qual masc,sing,gent"),
                                   morph.getTag("ADJF,Qual anim,masc,sing,accs")),
                     morph.tag("красивого"));
    }

    @Test
    public void test_getLexeme() throws IOException {
        List<ParsedWord> parseds;
        List<ParsedWord> lexeme;

        parseds = morph.parse("IV");
        assertParseds("IV:ROMN:IV:IV:0.5", parseds.get(0).getLexeme());
        assertParseds("IV:LATN:IV:IV:0.5", parseds.get(1).getLexeme());

        parseds = morph.parse("красивого");
        lexeme = parseds.get(0).getLexeme();
        assertParseds("красивый:ADJF,Qual masc,sing,nomn:красивый:красивый:1.0\n" +
                      "красивого:ADJF,Qual masc,sing,gent:красивый:красивого:1.0\n" +
                      "красивому:ADJF,Qual masc,sing,datv:красивый:красивому:1.0\n" +
                      "красивого:ADJF,Qual anim,masc,sing,accs:красивый:красивого:1.0\n" +
                      "красивый:ADJF,Qual inan,masc,sing,accs:красивый:красивый:1.0\n" +
                      "красивым:ADJF,Qual masc,sing,ablt:красивый:красивым:1.0\n" +
                      "красивом:ADJF,Qual masc,sing,loct:красивый:красивом:1.0\n" +
                      "красивая:ADJF,Qual femn,sing,nomn:красивый:красивая:1.0",
                      lexeme,
                      false);
        assertEquals(91, lexeme.size());

        // known prefix
        parseds = morph.parse("лжекот");
        lexeme = parseds.get(0).getLexeme();
        assertParseds("лжекот:NOUN,anim,masc sing,nomn:лжекот:кот:1.0\n" +
                      "лжекота:NOUN,anim,masc sing,gent:лжекот:кота:1.0",
                      lexeme,
                      false);
        assertEquals(12, lexeme.size());

        // unkown prefix
        parseds = morph.parse("лошарики");
        lexeme = parseds.get(0).getLexeme();
        assertParseds("лошарик:NOUN,inan,masc sing,nomn:лошарик:шарик:1.0\n" +
                      "лошарика:NOUN,inan,masc sing,gent:лошарик:шарика:1.0",
                      lexeme,
                      false);
        assertEquals(12, lexeme.size());
    }

    @Test
    public void test_inflect() throws IOException {
        List<ParsedWord> parseds;
        List<ParsedWord> paradigm;

        parseds = morph.parse("красивого");
        paradigm = parseds.get(0).inflect(Arrays.asList(morph.getGrammeme("ADJF"), morph.getGrammeme("femn")),
                                          Arrays.asList(morph.getGrammeme("Supr")));
        assertParseds("красивая:ADJF,Qual femn,sing,nomn:красивый:красивая:1.0\n" +
                      "красивой:ADJF,Qual femn,sing,gent:красивый:красивой:1.0\n" +
                      "красивой:ADJF,Qual femn,sing,datv:красивый:красивой:1.0\n" +
                      "красивую:ADJF,Qual femn,sing,accs:красивый:красивую:1.0\n" +
                      "красивой:ADJF,Qual femn,sing,ablt:красивый:красивой:1.0\n" +
                      "красивою:ADJF,Qual femn,sing,ablt,V-oy:красивый:красивою:1.0\n" +
                      "красивой:ADJF,Qual femn,sing,loct:красивый:красивой:1.0",
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
