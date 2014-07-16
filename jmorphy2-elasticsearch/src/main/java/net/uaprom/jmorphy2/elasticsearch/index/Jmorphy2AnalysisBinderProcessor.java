package net.uaprom.jmorphy2.elasticsearch.index;

import org.elasticsearch.index.analysis.AnalysisModule;


public class Jmorphy2AnalysisBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {
    // @Override
    // public void processAnalyzers(AnalyzersBindings analyzersBindings) {
    //     analyzersBindings.processAnalyzer("jmorphy2", Jmorphy2AnalyzerProvider.class);
    // }

    @Override
    public void processTokenFilters(TokenFiltersBindings tokenFiltersBindings) {
        tokenFiltersBindings.processTokenFilter("jmorphy2", Jmorphy2StemTokenFilterFactory.class);
    }
}
