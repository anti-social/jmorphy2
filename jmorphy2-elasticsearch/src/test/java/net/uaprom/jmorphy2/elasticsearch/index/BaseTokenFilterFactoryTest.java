package net.uaprom.jmorphy2.elasticsearch.index;

import org.elasticsearch.env.Environment;
import org.elasticsearch.env.EnvironmentModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNameModule;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.settings.IndexSettingsModule;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisModule;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.elasticsearch.test.ElasticsearchTestCase;

import net.uaprom.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin;


public class BaseTokenFilterFactoryTest extends ElasticsearchTestCase {
    protected AnalysisService createAnalysisService(Settings settings) {
        Index index = new Index("test");

        Injector parentInjector =
            new ModulesBuilder()
            .add(new SettingsModule(settings),
                 new EnvironmentModule(new Environment(settings)),
                 new IndicesAnalysisModule())
            .createInjector();

        AnalysisModule analysisModule =
            new AnalysisModule(settings,
                               parentInjector.getInstance(IndicesAnalysisService.class));
        AnalysisJmorphy2Plugin jmorphy2Plugin = new AnalysisJmorphy2Plugin();
        jmorphy2Plugin.onModule(analysisModule);

        Injector injector =
            new ModulesBuilder()
            .add(new IndexSettingsModule(index, settings),
                 new IndexNameModule(index),
                 analysisModule)
            .createChildInjector(parentInjector);

        return injector.getInstance(AnalysisService.class);
    }
}
