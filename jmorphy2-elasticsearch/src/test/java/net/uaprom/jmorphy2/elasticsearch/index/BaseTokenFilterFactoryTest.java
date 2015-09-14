package net.uaprom.jmorphy2.elasticsearch.index;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.EnvironmentModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNameModule;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.settings.IndexSettingsModule;
import org.elasticsearch.indices.IndicesModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.elasticsearch.test.ESTestCase;

import net.uaprom.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin;


public class BaseTokenFilterFactoryTest extends ESTestCase {
    protected AnalysisService createAnalysisService(Settings settings) {
        Index index = new Index("test");

        if (settings.get(IndexMetaData.SETTING_VERSION_CREATED) == null) {
            settings = Settings.builder().put(settings).put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT).build();
        }

        IndicesModule indicesModule = new IndicesModule(settings) {
            @Override
            public void configure() {
                // skip services
            }
        };
        Injector parentInjector =
            new ModulesBuilder()
            .add(new SettingsModule(settings),
                 new EnvironmentModule(new Environment(settings)),
                 indicesModule)
            .createInjector();

        AnalysisModule analysisModule =
            new AnalysisModule(settings,
                               parentInjector.getInstance(IndicesAnalysisService.class));
        new AnalysisJmorphy2Plugin().onModule(analysisModule);

        Injector injector =
            new ModulesBuilder()
            .add(new IndexSettingsModule(index, settings),
                 new IndexNameModule(index),
                 analysisModule)
            .createChildInjector(parentInjector);

        return injector.getInstance(AnalysisService.class);
    }
}
