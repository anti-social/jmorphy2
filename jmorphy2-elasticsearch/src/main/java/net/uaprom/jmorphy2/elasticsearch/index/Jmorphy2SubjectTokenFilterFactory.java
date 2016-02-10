package net.uaprom.jmorphy2.elasticsearch.index;

import java.util.Set;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AnalysisSettingsRequired;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.settings.IndexSettingsService;

import net.uaprom.jmorphy2.MorphAnalyzer;
import net.uaprom.jmorphy2.nlp.SubjectExtractor;
import net.uaprom.jmorphy2.lucene.Jmorphy2SubjectFilter;
import net.uaprom.jmorphy2.elasticsearch.indices.Jmorphy2Analysis;


@AnalysisSettingsRequired
public class Jmorphy2SubjectTokenFilterFactory extends AbstractTokenFilterFactory {
    private final SubjectExtractor subjExtractor;
    private final int maxSentenceLength;

    @Inject
    public Jmorphy2SubjectTokenFilterFactory(Index index,
                                             IndexSettingsService indexSettingsService,
                                             @Assisted String name,
                                             @Assisted Settings settings,
                                             Jmorphy2Analysis jmorphy2Service) {
        super(index, indexSettingsService.getSettings(), name, settings);

        String dictName = settings.get("name");
        subjExtractor = jmorphy2Service.getSubjectExtractor(dictName);

        maxSentenceLength = settings.getAsInt("max_sentence_length", 10);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2SubjectFilter(tokenStream, subjExtractor, maxSentenceLength);
    }
}
