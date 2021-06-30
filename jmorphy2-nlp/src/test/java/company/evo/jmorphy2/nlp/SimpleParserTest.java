package company.evo.jmorphy2.nlp;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.Jmorphy2TestsHelpers;


@RunWith(JUnit4.class)
public class SimpleParserTest {
    private Parser parser;
    private boolean initialized = false;

    private static final String TAGGER_RULES_RESOURCE = "/tagger_rules.txt";
    private static final String PARSER_RULES_RESOURCE = "/parser_rules.txt";

    @Before
    public void setUp() throws IOException {
        if (initialized) {
            return;
        }
        MorphAnalyzer morph = Jmorphy2TestsHelpers.newMorphAnalyzer("ru");
        parser = new SimpleParser(
            morph,
            new SimpleTagger(
                morph,
                new Ruleset(getClass().getResourceAsStream(TAGGER_RULES_RESOURCE))
            ),
            new Ruleset(getClass().getResourceAsStream(PARSER_RULES_RESOURCE)),
            100
        );
        initialized = true;
    }

    // @Test
    public void test() throws IOException {
        // System.out.println(parser.parse("Набор кухонной посуды из нержавеющей стали CALVE CL 1081".split(" ")).prettyToString(true));
        System.out.println(parser.parse("мягкая муз кукла майя в красном платье муз".split(" ")).prettyToString());

        // List<Node.Top> sents = parser.parseAll(new String[]{"чехол", "для", "телефона", "iphone"});
        // List<Node.Top> sents = parser.parseAll(new String[]{"женские", "сапоги"});
        // List<Node.Top> sents = parser.parseAll(new String[]{"уборка", "и", "вывоз", "снега", "и", "льда"});
        // List<Node.Top> sents = parser.parseAll(new String[]{"уборка", "снега", "и", "вывоз", "льда"});
        // List<Node.Top> sents = parser.parseAll("Набор кухонной посуды из нержавеющей стали CALVE CL 1081".split(" "));
        // List<Node.Top> sents = parser.parseAll("Проектирование систем полива".split(" "));
        // List<Node.Top> sents = parser.parseAll("Кнопка вызова официанта".split(" "));
        // List<Node.Top> sents = parser.parseAll("Видеокарта ATI Radeon HD5770 1Gb GDDR5 SAPPHIRE 11163 02 20R".split(" "));
        // List<Node.Top> sents = parser.parseAll("Масляной пилинг для рук Манго маракуйя 100 мл".split(" "));
        // List<Node.Top> sents = parser.parseAll("Продам МАЗ 5551 самосвал".split(" "));
        // List<Node.Top> sents = parser.parseAll("30 PIN ADAPTOR FOR IPHONE 5".split(" "));
        // List<Node.Top> sents = parser.parseAll("мягкая кукла майя в красном платье муз".split(" "));
        // for (Node.Top sent : sents) {
        //     System.out.println(sent.prettyToString());
        // }
    }

    @Test
    public void testParser() throws IOException {
        assertEquals("(TOP " +
                       "(NP,nomn,plur " +
                         "(ADJF,nomn,plur женские) " +
                         "(NOUN,inan,masc,nomn,plur сапоги)))",
                     parser.parse("женские сапоги".split(" ")).toString());

        assertEquals("(TOP " +
                       "(NP,nomn,plur " +
                         "(ADJF,nomn,plur женские) " +
                         "(NOUN,inan,masc,nomn,plur сапоги) " +
                         "(ADJF,Qual,nomn,plur коричневые)))",
                     parser.parse("женские сапоги коричневые".split(" ")).toString());

        assertEquals("(TOP " +
                       "(NP,nomn,sing " +
                         "(NP,nomn,sing " +
                           "(NP,nomn,sing " +
                             "(NOUN,inan,neut,nomn,sing проектирование)) " +
                           "(NP,gent,plur " +
                             "(NOUN,femn,gent,inan,plur систем))) " +
                         "(NP,gent,sing " +
                           "(NOUN,gent,inan,masc,sing полива))))",
                     parser.parse("проектирование систем полива".split(" ")).toString());

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
                       "(NP,nomn,sing " +
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
                       "(NP,nomn,sing " +
                         "(NP,nomn,sing " +
                           "(NP,nomn,sing " +
                             "(NOUN,femn,inan,nomn,sing уборка)) " +
                           "(NP,gent,sing " +
                             "(NOUN,gent,inan,masc,sing снега))) " +
                         "(CONJ и) " +
                         "(NP,nomn,sing " +
                           "(NP,nomn,sing " +
                             "(NOUN,inan,masc,nomn,sing вывоз)) " +
                           "(NP,gent,sing " +
                             "(NOUN,gent,inan,masc,sing льда)))))",
                     parser.parse(new String[]{"уборка", "снега", "и", "вывоз", "льда"}).toString());
    }
}
