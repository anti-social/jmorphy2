package net.uaprom.jmorphy2.solr;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.uaprom.jmorphy2.nlp.SubjectExtractor;


public class Jmorphy2SubjectFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);

    private final SubjectExtractor subjExtractor;
    private final int maxSentenceLength = 10;

    private Iterator<String> termsIterator = null;
    
    private static final Logger logger = LoggerFactory.getLogger(Jmorphy2SubjectFilter.class);

    public Jmorphy2SubjectFilter(TokenStream input, SubjectExtractor subjExtractor) {
        super(input);
        this.subjExtractor = subjExtractor;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (termsIterator != null && termsIterator.hasNext()) {
            // TODO: save and restore state for this token
            setTerm(termsIterator.next());
            return true;
        }

        List<String> terms = new ArrayList<String>(maxSentenceLength);
        int i = 0;
        while (i < maxSentenceLength && input.incrementToken()) {
            logger.info(String.format("%s", i));
            if (keywordAtt.isKeyword()) {
                logger.info(String.format("%s", keywordAtt.isKeyword()));
                continue;
            }
            terms.add(new String(termAtt.buffer(), 0, termAtt.length()));
            i++;
        }
        logger.info(String.format(">terms: %s", terms));
        // clearAttributes();

        List<String> subjTerms = subjExtractor.extract(terms.toArray(new String[0]));
        logger.info(String.format(">subjTerms: %s", subjTerms));
        termsIterator = subjTerms.iterator();
        if (!termsIterator.hasNext()) {
            return false;
        }
        setTerm(termsIterator.next());
        return true;
    }

    private void setTerm(String term) {
        logger.info(String.format("%s", term));
        termAtt.copyBuffer(term.toCharArray(), 0, term.length());
        termAtt.setLength(term.length());
        posIncAtt.setPositionIncrement(1);
    }
}
