package company.evo.jmorphy2.elasticsearch.index;

import org.elasticsearch.index.analysis.AnalysisModule;

import company.evo.jmorphy2.elasticsearch.index.Jmorphy2StemTokenFilterFactory;
import company.evo.jmorphy2.elasticsearch.index.Jmorphy2SubjectTokenFilterFactory;


public class Jmorphy2AnalysisBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {
    @Override
    public void processTokenFilters(TokenFiltersBindings tokenFiltersBindings) {
        tokenFiltersBindings.processTokenFilter("jmorphy2_stemmer", Jmorphy2StemTokenFilterFactory.class);
        tokenFiltersBindings.processTokenFilter("jmorphy2_subject", Jmorphy2SubjectTokenFilterFactory.class);
    }
}
