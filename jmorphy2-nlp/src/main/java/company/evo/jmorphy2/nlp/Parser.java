package company.evo.jmorphy2.nlp;

import java.io.IOException;
import java.util.List;

import company.evo.jmorphy2.MorphAnalyzer;


public abstract class Parser {
    protected final MorphAnalyzer morph;
    protected final Tagger tagger;

    public Parser(MorphAnalyzer morph, Tagger tagger) {
        this.morph = morph;
        this.tagger = tagger;
    }

    public Node.Top parse(String[] tokens) throws IOException {
        return parse(tagger.tagAll(tokens));
    }

    public abstract Node.Top parse(List<Node.Top> taggedSents) throws IOException;

    public List<Node.Top> parseAll(String[] tokens) throws IOException {
        return parseAll(tagger.tagAll(tokens));
    }

    public abstract List<Node.Top> parseAll(List<Node.Top> taggedSents) throws IOException;
}
