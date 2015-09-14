package net.uaprom.jmorphy2.elasticsearch.plugin;

import java.util.Collection;
import java.util.Collections;

import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.Plugin;

import net.uaprom.jmorphy2.elasticsearch.index.Jmorphy2AnalysisBinderProcessor;
import net.uaprom.jmorphy2.elasticsearch.indices.Jmorphy2AnalysisModule;


public class AnalysisJmorphy2Plugin extends Plugin {
    @Override
    public String name() {
        return "analysis-jmorphy2";
    }

    @Override
    public String description() {
        return "Jmorphy2 analysis plugin";
    }

    @Override
    public Collection<Module> nodeModules() {
        return Collections.<Module>singletonList(new Jmorphy2AnalysisModule());
    }

    public void onModule(AnalysisModule module) {
        module.addProcessor(new Jmorphy2AnalysisBinderProcessor());
    }
}
