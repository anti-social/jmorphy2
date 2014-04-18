package net.uaprom.jmorphy2.solr;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import net.uaprom.jmorphy2.nlp.SubjectExtractor;


public class Jmorphy2SubjectFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);

    private final SubjectExtractor subjExtractor;

    public Jmorphy2SubjectFilter(TokenStream input, SubjectExtractor subjExtractor) {
        super(input);
        this.subjExtractor = subjExtractor;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        List<String> tokens = new ArrayList<String>();
        while(input.incrementToken()) {
            if (keywordAtt.isKeyword()) {
                continue;
            }
            tokens.add(new String(termAtt.buffer(), 0, termAtt.length()));
        }

        List<String> subjTokens = subjExtractor.extract(tokens.toArray(new String[0]));
        for (String t : subjTokens) {
            // TODO: save and restore state
            termAtt.copyBuffer(t.toCharArray(), 0, t.length());
            termAtt.setLength(t.length());
            return true;
        }
        return false;
    }
}
