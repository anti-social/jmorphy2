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

import net.uaprom.jmorphy2.Parsed;
import net.uaprom.jmorphy2.MorphAnalyzer;


public class Jmorphy2StemFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);

    private final MorphAnalyzer morph;
    private final List<Set<String>> includeTags;
    private final boolean includeUnknown;
        
    private List<String> normalForms = null;
    private State savedState = null;
    private boolean first = true;
     
    public Jmorphy2StemFilter(TokenStream input, MorphAnalyzer morph, List<Set<String>> includeTags, boolean includeUnknown) {
        super(input);
        this.morph = morph;
        this.includeTags = includeTags;
        this.includeUnknown = includeUnknown;
    }
        
    @Override
    public boolean incrementToken() throws IOException {
        if (normalForms != null && !normalForms.isEmpty()) {
            String nextStem = normalForms.remove(0);
            restoreState(savedState);
            posIncAtt.setPositionIncrement(0);
            termAtt.copyBuffer(nextStem.toCharArray(), 0, nextStem.length());
            termAtt.setLength(nextStem.length());
            return true;
        }

        while (input.incrementToken()) {
            if (keywordAtt.isKeyword()) {
                return true;
            }

            normalForms = getNormalForms(termAtt);

            if (normalForms.isEmpty()) {
                continue;
            }

            String stem = normalForms.remove(0);
            termAtt.copyBuffer(stem.toCharArray(), 0, stem.length());
            termAtt.setLength(stem.length());

            if (!normalForms.isEmpty()) {
                savedState = captureState();
            }

            if (first && posIncAtt.getPositionIncrement() == 0) {
                posIncAtt.setPositionIncrement(1);
                first = false;
            }
            return true;
        }
        return false;
    }

    private List<String> getNormalForms(CharTermAttribute termAtt) throws IOException {
        List<String> normalForms = new ArrayList<String>();
        Set<String> uniqueNormalForms = new HashSet<String>();

        char[] termBuffer = termAtt.buffer();
        int termLength = termAtt.length();
        String token = new String(termBuffer, 0, termLength);
        
        List<Parsed> parseds = morph.parse(token);

        if (parseds.isEmpty()) {
            if (includeUnknown) {
                normalForms.add(token);
            }
        } else if (includeTags == null) {
            for (Parsed p : parseds) {
                if (!uniqueNormalForms.contains(p.normalForm)) {
                    normalForms.add(p.normalForm);
                    uniqueNormalForms.add(p.normalForm);
                }
            }
        } else {
            for (Parsed p : parseds) {
                for (Set<String> grammemeValues : includeTags) {
                    if (p.tag.containsAllValues(grammemeValues)) {
                        if (!uniqueNormalForms.contains(p.normalForm)) {
                            normalForms.add(p.normalForm);
                            uniqueNormalForms.add(p.normalForm);
                            break;
                        }
                    }
                }
            }
        }

        return normalForms;
    }
}
