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
public class MorphAnalyzerRUTest {
    private MorphAnalyzer morph;
    private boolean initialized = false;

    @Before
    public void setUp() throws IOException {
        if (!initialized) {
            morph = Jmorphy2TestsHelpers.newMorphAnalyzer("ru");
            initialized = true;
        }
    }

    @Test
    public void test_parse() throws IOException {
        List<ParsedWord> parseds;

        parseds = morph.parse("красивого");
        Tag tag = parseds.get(0).tag;
        assertParseds("красивого:ADJF,Qual neut,sing,gent:красивый:красивого:0.666666\n" +
                      "красивого:ADJF,Qual masc,sing,gent:красивый:красивого:0.166666\n" +
                      "красивого:ADJF,Qual anim,masc,sing,accs:красивый:красивого:0.166666",
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

        assertParseds("сегодня:ADVB:сегодня:сегодня:1.0", morph.parse("сегодня"), true);

        // dictionary word: capitalized
        assertParseds("украину:NOUN,inan,femn,Sgtm,Geox sing,accs:украина:украину:1.0",
                      morph.parse("Украину"));

        // known prefix
        assertParseds("псевдокошка:NOUN,anim,femn sing,nomn:псевдокошка:кошка:0.9375\n" +
                      "псевдокошка:NOUN,inan,femn sing,nomn:псевдокошка:кошка:0.0625",
                      morph.parse("псевдокошка"));
        assertParseds("лже-кот:NOUN,anim,masc sing,nomn:лже-кот:кот:1.0",
                      morph.parse("лже-кот"));

        // unknown prefix
        assertParseds("лошарики:NOUN,inan,masc plur,nomn:лошарик:шарики:0.2\n" +
                      "лошарики:NOUN,inan,masc plur,accs:лошарик:шарики:0.2\n" +
                      "лошарики:NOUN,anim,masc,Name plur,nomn:лошарик:арики:0.2\n" +
                      "лошарики:NOUN,anim,femn,Name sing,gent:лошарика:арики:0.2\n" +
                      "лошарики:NOUN,anim,femn,Name plur,nomn:лошарика:арики:0.2\n",
                      morph.parse("лошарики"));

        // unknown prefix: maximum prefix length
        assertParseds("бочкоподобный:ADJF,Subx,Qual masc,sing,nomn:бочкоподобный:подобный:0.833333\n" +
                      "бочкоподобный:ADJF,Subx,Qual inan,masc,sing,accs:бочкоподобный:подобный:0.166666\n",
                      morph.parse("бочкоподобный"));

        // unknown prefix: minimum reminder
        assertParseds("штрихкот:NOUN,anim,masc sing,nomn:штрихкот:кот:1.0\n",
                      morph.parse("штрихкот"));

        assertParseds("снега:NOUN,inan,masc sing,gent:снег:снега:0.777777\n" +
                      "снега:NOUN,inan,masc plur,nomn:снег:снега:0.166666\n" +
                      "снега:NOUN,inan,masc plur,accs:снег:снега:0.055555\n",
                      morph.parse("снега"));

        // known suffix
        assertParseds("няшка:NOUN,inan,femn sing,nomn:няшка:няшка:1.0\n",
                      morph.parse("няшка"));
        assertParseds("шуруповерт:NOUN,anim,masc sing,nomn:шуруповерт:оверт:1.0\n",
                      morph.parse("шуруповерт"));
        assertParseds("шуруповертами:NOUN,inan,masc plur,ablt:шуруповерт:ртами:1.0\n",
                      morph.parse("шуруповертами"));

        // known suffix: with paradigm prefix
        assertParseds("наиняшнейший:ADJF,Supr,Qual masc,sing,nomn:наиняшный:ейший:0.252275\n" +
                      "наиняшнейший:ADJF,Supr,Qual inan,masc,sing,accs:наиняшный:ейший:0.252275\n" +
                      "наиняшнейший:ADJF,Supr,Qual masc,sing,nomn:няшный:ейший:0.246087\n" +
                      "наиняшнейший:ADJF,Supr,Qual inan,masc,sing,accs:няшный:ейший:0.246087\n" +
                      "наиняшнейший:NOUN,anim,masc sing,nomn:наиняшнейший:ейший:0.003276\n",
                      morph.parse("наиняшнейший"));

        // gen2, loct, loc2
        assertParseds("снеге:NOUN,inan,masc sing,loct:снег:снеге:1.0", morph.parse("снеге"));
        assertParseds("снегу:NOUN,inan,masc sing,loc2:снег:снегу:0.555555\n" +
                      "снегу:NOUN,inan,masc sing,datv:снег:снегу:0.333333\n" +
                      "снегу:NOUN,inan,masc sing,gen2:снег:снегу:0.111111\n",
                      morph.parse("снегу"));

        // е, ё
        assertParseds("ёжик:NOUN,anim,masc sing,nomn:ёжик:ёжик:1.0", morph.parse("ёжик"));
        assertParseds("ежик:NOUN,anim,masc sing,nomn:ёжик:ёжик:1.0", morph.parse("ежик"));
        assertParseds("теплые:ADJF,Qual plur,nomn:тёплый:тёплые:0.6\n" +
                      "теплые:ADJF,Qual inan,plur,accs:тёплый:тёплые:0.4",
                      morph.parse("теплые"));

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

        // ROMN (all roman numbers are also latin)
        assertParseds("MD:ROMN:MD:MD:0.5\n" +
                      "MD:LATN:MD:MD:0.5\n",
                      morph.parse("MD"));

        // Unknown word
        assertParseds("ъь:UNKN:ъь:ъь:1.0", morph.parse("ъь"));
        assertParseds("тестsymbolmix:UNKN:тестsymbolmix:тестsymbolmix:1.0",
                      morph.parse("тестsymbolmix"));

        // TODO: Hyphen
    }

    @Test
    public void test_normalForms() throws IOException {
        assertEquals(Arrays.asList("красивый"), morph.normalForms("красивого"));
        assertEquals(Arrays.asList("для", "длить"), morph.normalForms("для"));
        assertEquals(Arrays.asList("лошарик", "лошарика"), morph.normalForms("лошарикам"));
    }

    @Test
    public void test_getTag() throws IOException {
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
        assertParseds("красивый:ADJF,Qual masc,sing,nomn:красивый:красивый:0.666666\n" +
                      "красивого:ADJF,Qual masc,sing,gent:красивый:красивого:0.666666\n" +
                      "красивому:ADJF,Qual masc,sing,datv:красивый:красивому:0.666666\n" +
                      "красивого:ADJF,Qual anim,masc,sing,accs:красивый:красивого:0.666666\n" +
                      "красивый:ADJF,Qual inan,masc,sing,accs:красивый:красивый:0.666666\n" +
                      "красивым:ADJF,Qual masc,sing,ablt:красивый:красивым:0.666666\n" +
                      "красивом:ADJF,Qual masc,sing,loct:красивый:красивом:0.666666\n" +
                      "красивая:ADJF,Qual femn,sing,nomn:красивый:красивая:0.666666",
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

        // known suffix
        parseds = morph.parse("шуруповертами");
        lexeme = parseds.get(0).getLexeme();
        assertParseds("шуруповерт:NOUN,inan,masc sing,nomn:шуруповерт:ртами:1.0\n" +
                      "шуруповерта:NOUN,inan,masc sing,gent:шуруповерт:ртами:1.0\n",
                      lexeme,
                      false);
        assertEquals(12, lexeme.size());

        // Unknown word
        assertParseds("ъь:UNKN:ъь:ъь:1.0", morph.parse("ъь").get(0).getLexeme());
    }

    @Test
    public void test_inflect_includeGrammemes() throws IOException {
        List<ParsedWord> parseds;
        List<ParsedWord> paradigm;

        parseds = morph.parse("красивого");
        paradigm = parseds.get(0)
                .inflect(
                        Arrays.asList(
                                morph.getGrammeme("ADJF"),
                                morph.getGrammeme("femn"),
                                morph.getGrammeme("nomn")
                        )
                );
        assertParseds(
                "красивая:ADJF,Qual femn,sing,nomn:красивый:красивая:0.666666\n" +
                        "красивейшая:ADJF,Supr,Qual femn,sing,nomn:красивый:красивейшая:0.666666\n" +
                        "наикрасивейшая:ADJF,Supr,Qual femn,sing,nomn:красивый:наикрасивейшая:0.666666",
                paradigm
        );
    }

    @Test
    public void test_inflect_includeAndExcludeGrammemes() throws IOException {
        List<ParsedWord> parseds;
        List<ParsedWord> paradigm;

        parseds = morph.parse("красивого");
        paradigm = parseds.get(0)
                .inflect(
                        Arrays.asList(morph.getGrammeme("ADJF"), morph.getGrammeme("femn")),
                        Arrays.asList(morph.getGrammeme("Supr"))
                );
        assertParseds("красивая:ADJF,Qual femn,sing,nomn:красивый:красивая:0.666666\n" +
                      "красивой:ADJF,Qual femn,sing,gent:красивый:красивой:0.666666\n" +
                      "красивой:ADJF,Qual femn,sing,datv:красивый:красивой:0.666666\n" +
                      "красивую:ADJF,Qual femn,sing,accs:красивый:красивую:0.666666\n" +
                      "красивой:ADJF,Qual femn,sing,ablt:красивый:красивой:0.666666\n" +
                      "красивою:ADJF,Qual femn,sing,ablt,V-oy:красивый:красивою:0.666666\n" +
                      "красивой:ADJF,Qual femn,sing,loct:красивый:красивой:0.666666",
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
