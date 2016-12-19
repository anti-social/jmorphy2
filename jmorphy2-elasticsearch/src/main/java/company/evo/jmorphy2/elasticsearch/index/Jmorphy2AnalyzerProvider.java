package company.evo.jmorphy2.elasticsearch.index;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;

import company.evo.jmorphy2.lucene.Jmorphy2Analyzer;


public class Jmorphy2AnalyzerProvider extends AbstractIndexAnalyzerProvider<Jmorphy2Analyzer> {

    private final Jmorphy2Analyzer analyzer;

    public Jmorphy2AnalyzerProvider(IndexSettings indexSettings,
                                    Environment environment,
                                    String name,
                                    Settings settings) {
        super(indexSettings, name, settings);
        // analyzer = new Jmorphy2Analyzer(version);
        analyzer = null;
    }


    @Override
    public Jmorphy2Analyzer get() {
        return analyzer;
    }
}
