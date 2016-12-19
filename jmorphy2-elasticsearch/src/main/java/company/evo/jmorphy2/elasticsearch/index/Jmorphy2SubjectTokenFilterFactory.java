package company.evo.jmorphy2.elasticsearch.index;

import java.util.Set;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.analysis.TokenStream;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.nlp.SubjectExtractor;
import company.evo.jmorphy2.lucene.Jmorphy2SubjectFilter;
import company.evo.jmorphy2.elasticsearch.indices.Jmorphy2Service;


public class Jmorphy2SubjectTokenFilterFactory extends AbstractTokenFilterFactory {
    private final SubjectExtractor subjExtractor;
    private final int maxSentenceLength;

    public Jmorphy2SubjectTokenFilterFactory(IndexSettings indexSettings,
                                             Environment environment,
                                             String name,
                                             Settings settings,
                                             Jmorphy2Service jmorphy2Service) {
        super(indexSettings, name, settings);

        String lang = settings.get("name");
        if (lang == null) {
            throw new IllegalArgumentException
                ("Missing [lang] configuration for jmorphy2 subject token filter");
        }
        subjExtractor = jmorphy2Service.getSubjectExtractor(lang);
        if (subjExtractor == null) {
            throw new IllegalArgumentException
                (String.format(Locale.ROOT, "Cannot find subject extractor for lang: %s", lang));
        }

        maxSentenceLength = settings.getAsInt("max_sentence_length", 10);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2SubjectFilter(tokenStream, subjExtractor, maxSentenceLength);
    }
}
