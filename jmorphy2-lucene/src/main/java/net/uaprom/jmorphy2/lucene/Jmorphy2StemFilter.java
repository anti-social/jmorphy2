package net.uaprom.jmorphy2.lucene;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import net.uaprom.jmorphy2.ParsedWord;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class Jmorphy2StemFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);

    private final MorphAnalyzer morph;
    private final List<Set<String>> excludeTags;
    private final List<Set<String>> includeTags;
    private final boolean includeUnknown;
    private final boolean enablePositionIncrements;
        
    private Iterator<String> normalForms = null;
    private State savedState = null;
    private boolean first = true;
    private int skippedPositions = 0;
     
    public Jmorphy2StemFilter(TokenStream input, MorphAnalyzer morph) {
        this(input, morph, null, null, true, true);
    }

    public Jmorphy2StemFilter(TokenStream input,
                              MorphAnalyzer morph,
                              List<Set<String>> excludeTags,
                              List<Set<String>> includeTags,
                              boolean includeUnknown) {
        this(input, morph, excludeTags, includeTags, includeUnknown, true);
    }

    public Jmorphy2StemFilter(TokenStream input,
                              MorphAnalyzer morph,
                              List<Set<String>> excludeTags,
                              List<Set<String>> includeTags,
                              boolean includeUnknown,
                              boolean enablePositionIncrements) {
        super(input);
        this.morph = morph;
        this.excludeTags = excludeTags;
        this.includeTags = includeTags;
        this.includeUnknown = includeUnknown;
        this.enablePositionIncrements = enablePositionIncrements;
    }
    
    @Override
    public void reset() throws IOException {
        super.reset();
        normalForms = null;
        first = true;
        skippedPositions = 0;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (normalForms == null || !normalForms.hasNext()) {
            skippedPositions = 0;

            while (input.incrementToken()) {
                if (keywordAtt.isKeyword()) {
                    return true;
                }

                normalForms = getNormalForms(termAtt).iterator();

                if (normalForms.hasNext()) {
                    setTerm(normalForms.next(), posIncAtt.getPositionIncrement());
                    if (normalForms.hasNext()) {
                        savedState = captureState();
                    }
                    skippedPositions = 0;
                    return true;
                }

                skippedPositions += posIncAtt.getPositionIncrement();
            }

            return false;
        }

        restoreState(savedState);
        setTerm(normalForms.next(), 0);
        return true;
    }

    private List<String> getNormalForms(CharTermAttribute termAtt) throws IOException {
        List<String> normalForms = new ArrayList<String>();
        Set<String> uniqueNormalForms = new HashSet<String>();

        char[] termBuffer = termAtt.buffer();
        int termLength = termAtt.length();
        String token = new String(termBuffer, 0, termLength);
        
        List<ParsedWord> parseds = morph.parse(token);

        if (parseds.isEmpty()) {
            if (includeUnknown) {
                normalForms.add(token);
            }
        } else {
            for (ParsedWord p : parseds) {
                boolean shouldAdd = false;
                if (includeTags != null) {
                    for (Set<String> includeGrammemeValues : includeTags) {
                        if (p.tag.containsAllValues(includeGrammemeValues)) {
                            shouldAdd = true;
                            break;
                        }
                    }
                } else if (excludeTags != null) {
                    boolean shouldExclude = false;
                    for (Set<String> excludeGrammemeValues : excludeTags) {
                        if (p.tag.containsAllValues(excludeGrammemeValues)) {
                            shouldExclude = true;
                            break;
                        }
                    }
                    if (!shouldExclude) {
                        shouldAdd = true;
                    }
                } else {
                    shouldAdd = true;
                }

                if (shouldAdd && !uniqueNormalForms.contains(p.normalForm)) {
                    normalForms.add(p.normalForm);
                    uniqueNormalForms.add(p.normalForm);
                }
            }
        }

        return normalForms;
    }

    private void setTerm(String stem, int posInc) {
        termAtt.copyBuffer(stem.toCharArray(), 0, stem.length());
        termAtt.setLength(stem.length());

        if (enablePositionIncrements) {
            posIncAtt.setPositionIncrement(posInc + skippedPositions);
        } else {
            if (first) {
                if (posInc == 0) {
                    posIncAtt.setPositionIncrement(1);
                }
                first = false;
            } else {
                posIncAtt.setPositionIncrement(posInc);
            }
        }
    }
}
