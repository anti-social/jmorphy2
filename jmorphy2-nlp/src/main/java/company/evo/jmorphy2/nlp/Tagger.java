package company.evo.jmorphy2.nlp;

import java.io.IOException;
import java.util.List;

import company.evo.jmorphy2.MorphAnalyzer;


public abstract class Tagger {
    protected final MorphAnalyzer morph;

    public Tagger(MorphAnalyzer morph) {
        this.morph = morph;
    }

    public abstract Node.Top tag(String[] tokens) throws IOException;

    public abstract List<Node.Top> tagAll(String[] tokens) throws IOException;
}
