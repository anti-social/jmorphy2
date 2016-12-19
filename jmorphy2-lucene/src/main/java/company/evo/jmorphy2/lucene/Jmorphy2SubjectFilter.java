package company.evo.jmorphy2.lucene;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import company.evo.jmorphy2.nlp.SubjectExtractor;


public class Jmorphy2SubjectFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);

    private final SubjectExtractor subjExtractor;
    private final int maxSentenceLength;

    private Iterator<SubjectExtractor.Token> subjTokensIterator = null;
    // private Iterator<Integer> posIncIterator = null;
    private List<State> savedStates = null;

    private int prevIndex = -1;

    public Jmorphy2SubjectFilter(TokenStream input, SubjectExtractor subjExtractor) {
        this(input, subjExtractor, Jmorphy2SubjectFilterFactory.DEFAULT_MAX_SENTENCE_LENGTH);
    }

    public Jmorphy2SubjectFilter(TokenStream input, SubjectExtractor subjExtractor, int maxSentenceLength) {
        super(input);
        this.subjExtractor = subjExtractor;
        this.maxSentenceLength = maxSentenceLength;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        subjTokensIterator = null;
        // posIncIterator = null;
        savedStates = null;
        prevIndex = -1;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (subjTokensIterator == null) {
            List<String> terms = new ArrayList<String>(maxSentenceLength);
            // List<Integer> positions = new ArrayList<Integer>(maxSentenceLength);
            savedStates = new ArrayList<State>(maxSentenceLength);
            while (terms.size() < maxSentenceLength && input.incrementToken()) {
                if (keywordAtt.isKeyword()) {
                    continue;
                }
                terms.add(new String(termAtt.buffer(), 0, termAtt.length()));
                // positions.add(posIncAtt.getPositionIncrement());
                savedStates.add(captureState());
            }
            // System.out.println(terms);
            // (new Throwable()).printStackTrace(System.out);
            List<SubjectExtractor.Token> subjTerms =
                subjExtractor.extractTokens(terms.toArray(new String[0]));
            subjTokensIterator = subjTerms.iterator();
        }

        if (!subjTokensIterator.hasNext()) {
            return false;
        }
        setTerm(subjTokensIterator.next());
        return true;
    }

    private void setTerm(SubjectExtractor.Token token) {
        restoreState(savedStates.get(token.index));
        termAtt.copyBuffer(token.word.toCharArray(), 0, token.word.length());
        termAtt.setLength(token.word.length());
        posIncAtt.setPositionIncrement(posIncAtt.getPositionIncrement() - 1 + token.index - prevIndex);
        prevIndex = token.index;
    }
}
