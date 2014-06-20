package net.uaprom.jmorphy2.solr;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Set;
import java.util.List;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;

import net.uaprom.jmorphy2.test._BaseTestCase;


@RunWith(JUnit4.class)
public class Jmorphy2StemFilterTest extends BaseFilterTestCase {
    @Before
    public void setUp() throws IOException {
        initMorphAnalyzer();
    }

    @Override
    protected TokenFilter getTokenFilter(TokenStream source) {
        List<Set<String>> includeTags =
            ImmutableList.<Set<String>>of(ImmutableSet.of("NOUN"),
                                          ImmutableSet.of("ADJF"),
                                          ImmutableSet.of("ADJS"),
                                          ImmutableSet.of("LATN"),
                                          ImmutableSet.of("NUMB"));

        return new Jmorphy2StemFilter(source, morph, includeTags, true);
    }

    @Test
    public void test() throws IOException {
        Analyzer analyzer = getAnalyzer();

        assertAnalyzesTo(analyzer,
                         "",
                         Arrays.asList(new String[0]),
                         Arrays.asList(new Integer[0]));
        assertAnalyzesTo(analyzer,
                         "iphone",
                         Arrays.asList(new String[]{"iphone"}),
                         Arrays.asList(new Integer[]{1}));
        assertAnalyzesTo(analyzer,
                         "теплые перчатки",
                         Arrays.asList(new String[]{"тёплый", "перчатка"}),
                         Arrays.asList(new Integer[]{1, 1}));
        assertAnalyzesTo(analyzer,
                         "магнит на холодильник",
                         Arrays.asList(new String[]{"магнит", "холодильник"}),
                         Arrays.asList(new Integer[]{1, 2}));
        // assertAnalyzesTo(analyzer,
        //                  "чехол кожаный 5 for iphone 4",
        //                  Arrays.asList(new String[]{"чехол", "5"}),
        //                  Arrays.asList(new Integer[]{1, 2}));
    }
}
