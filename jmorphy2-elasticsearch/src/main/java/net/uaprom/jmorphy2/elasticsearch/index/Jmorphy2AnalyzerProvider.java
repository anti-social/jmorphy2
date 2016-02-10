package net.uaprom.jmorphy2.elasticsearch.index;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;
import org.elasticsearch.index.settings.IndexSettingsService;

import net.uaprom.jmorphy2.lucene.Jmorphy2Analyzer;


public class Jmorphy2AnalyzerProvider extends AbstractIndexAnalyzerProvider<Jmorphy2Analyzer> {
    private final Jmorphy2Analyzer analyzer;

    @Inject
    public Jmorphy2AnalyzerProvider(Index index,
                                    IndexSettingsService indexSettingsService,
                                    Environment environment,
                                    @Assisted String name,
                                    @Assisted Settings settings) {
        super(index, indexSettingsService.getSettings(), name, settings);
        // analyzer = new Jmorphy2Analyzer(version);
        analyzer = null;
    }


    @Override
    public Jmorphy2Analyzer get() {
        return analyzer;
    }
}
