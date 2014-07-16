package net.uaprom.jmorphy2.elasticsearch.indices;

import org.elasticsearch.common.inject.AbstractModule;


public class Jmorphy2AnalysisModule extends AbstractModule {
    @Override
    public void configure() {
        bind(Jmorphy2Analysis.class).asEagerSingleton();
        // bind(Jmorphy2Service.class).asEagerSingleton();
    }
}
