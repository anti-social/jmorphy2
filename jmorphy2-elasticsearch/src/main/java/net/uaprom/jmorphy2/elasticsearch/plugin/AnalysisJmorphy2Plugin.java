package net.uaprom.jmorphy2.elasticsearch.plugin;

import java.util.Collection;

import org.elasticsearch.common.collect.ImmutableList;

import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.common.inject.Module;
// import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.plugins.AbstractPlugin;

// import net.uaprom.jmorphy2.elasticsearch.index.Jmorphy2AnalysisBinderProcessor;
import net.uaprom.jmorphy2.elasticsearch.index.Jmorphy2StemTokenFilterFactory;
import net.uaprom.jmorphy2.elasticsearch.index.Jmorphy2SubjectTokenFilterFactory;
import net.uaprom.jmorphy2.elasticsearch.indices.Jmorphy2AnalysisModule;


public class AnalysisJmorphy2Plugin extends AbstractPlugin {
    @Override
    public String name() {
        return "analysis-jmorphy2";
    }

    @Override
    public String description() {
        return "Jmorphy2 analysis plugin";
    }

    // @Override
    // public Collection<Class<? extends LifecycleComponent>> services() {
    //     return ImmutableList.<Class<? extends LifecycleComponent>>of(Jmorphy2Service.class);
    // }

    @Override
    public Collection<Class<? extends Module>> modules() {
        return ImmutableList.<Class<? extends Module>>of(Jmorphy2AnalysisModule.class);
    }

    public void onModule(AnalysisModule module) {
        // module.addProcessor(new Jmorphy2AnalysisBinderProcessor());
        module.addTokenFilter("jmorphy2_stemmer", Jmorphy2StemTokenFilterFactory.class);
        module.addTokenFilter("jmorphy2_subject", Jmorphy2SubjectTokenFilterFactory.class);
    }
}
