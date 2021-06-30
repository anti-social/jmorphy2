package company.evo.jmorphy2.lucene;

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

import company.evo.jmorphy2.Grammeme;
import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.ParsedWord;


public class Jmorphy2StemFilter extends TokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);

    private final MorphAnalyzer morph;
    private final List<Set<Grammeme>> includeGrammemes;
    private final List<Set<Grammeme>> excludeGrammemes;
    private final boolean enablePositionIncrements;

    private Iterator<String> normalForms = null;
    private State savedState = null;
    private boolean first = true;
    private int skippedPositions = 0;

    public Jmorphy2StemFilter(TokenStream input, MorphAnalyzer morph) {
        this(input, morph, null, null, true);
    }

    public Jmorphy2StemFilter(TokenStream input, MorphAnalyzer morph, List<Set<String>> includeTags) {
        this(input, morph, includeTags, null, true);
    }

    public Jmorphy2StemFilter(TokenStream input,
                              MorphAnalyzer morph,
                              List<Set<String>> includeTags,
                              List<Set<String>> excludeTags) {
        this(input, morph, includeTags, excludeTags, true);
    }

    public Jmorphy2StemFilter(TokenStream input,
                              MorphAnalyzer morph,
                              List<Set<String>> includeTags,
                              List<Set<String>> excludeTags,
                              boolean enablePositionIncrements) {
        super(input);
        this.morph = morph;
        this.includeGrammemes = convertValuesToGrammemes(includeTags);
        this.excludeGrammemes = convertValuesToGrammemes(excludeTags);
        this.enablePositionIncrements = enablePositionIncrements;
    }

    private List<Set<Grammeme>> convertValuesToGrammemes(List<Set<String>> valuesSets) {
        if (valuesSets == null) {
            return null;
        }

        List<Set<Grammeme>> grammemesSets = new ArrayList<>();
        for (Set<String> valueSet : valuesSets) {
            Set<Grammeme> grammemeSet = new HashSet<>();
            for (String value : valueSet) {
                grammemeSet.add(morph.getGrammeme(value));
            }

            if (!grammemeSet.isEmpty()) {
                grammemesSets.add(grammemeSet);
            }
        }

        return grammemesSets;
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

    private List<String> getNormalForms(CharTermAttribute termAtt) {
        List<String> normalForms = new ArrayList<>();
        Set<String> uniqueNormalForms = new HashSet<>();

        char[] termBuffer = termAtt.buffer();
        int termLength = termAtt.length();
        String token = new String(termBuffer, 0, termLength);

        List<ParsedWord> parseds = morph.parse(token);

        for (ParsedWord p : parseds) {
            boolean shouldAdd = false;
            if (includeGrammemes != null) {
                for (Set<Grammeme> includeGrammemeSet : includeGrammemes) {
                    if (p.tag.containsAll(includeGrammemeSet)) {
                        shouldAdd = true;
                        break;
                    }
                }
            } else if (excludeGrammemes != null) {
                boolean shouldExclude = false;
                for (Set<Grammeme> excludeGrammemeSet : excludeGrammemes) {
                    if (p.tag.containsAll(excludeGrammemeSet)) {
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
