package company.evo.jmorphy2.elasticsearch.index;

import java.util.Collection;

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
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.ESIntegTestCase;

import company.evo.jmorphy2.elasticsearch.index.Jmorphy2AnalysisBinderProcessor;
import company.evo.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin;


public class BaseTokenFilterFactoryTest extends ESIntegTestCase {
    protected AnalysisService createAnalysisService(Settings settings) {
        Index index = new Index("test");

        if (settings.get(IndexMetaData.SETTING_VERSION_CREATED) == null) {
            settings = Settings.builder()
                .put(settings)
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .build();
        }

        Injector parentInjector = new ModulesBuilder()
            .add(new SettingsModule(settings),
                 new EnvironmentModule(new Environment(settings)))
            .createInjector();
        Injector injector = new ModulesBuilder()
            .add(new IndexSettingsModule(index, settings),
                 new IndexNameModule(index),
                 new AnalysisModule(settings,
                                    parentInjector.getInstance(IndicesAnalysisService.class))
                 .addProcessor(new Jmorphy2AnalysisBinderProcessor()))
            .createChildInjector(parentInjector);

        return injector.getInstance(AnalysisService.class);
    }
}
