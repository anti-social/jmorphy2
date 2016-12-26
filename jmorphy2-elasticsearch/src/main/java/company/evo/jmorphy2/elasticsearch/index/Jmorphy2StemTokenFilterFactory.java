package company.evo.jmorphy2.elasticsearch.index;

import java.io.IOException;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.lucene.Jmorphy2StemFilter;
import company.evo.jmorphy2.lucene.Jmorphy2StemFilterFactory;
import static company.evo.jmorphy2.lucene.Jmorphy2StemFilterFactory.parseTags;
import company.evo.jmorphy2.elasticsearch.indices.Jmorphy2Service;


public class Jmorphy2StemTokenFilterFactory extends AbstractTokenFilterFactory {
    private final MorphAnalyzer morph;

    private final List<Set<String>> includeTags;
    private final List<Set<String>> excludeTags;

    public Jmorphy2StemTokenFilterFactory(IndexSettings indexSettings,
                                          Environment environment,
                                          String name,
                                          Settings settings,
                                          Jmorphy2Service jmorphy2Service) {
        super(indexSettings, name, settings);

        // System.out.println(name);
        String lang = settings.get("name");
        if (lang == null) {
            throw new IllegalArgumentException
                ("Missing [lang] configuration for jmorphy2 token filter");
        }
        morph = jmorphy2Service.getMorphAnalyzer(lang);
        if (morph == null) {
            throw new IllegalArgumentException
                (String.format(Locale.ROOT, "Cannot find dictionary for lang: %s", lang));
        }
        includeTags = parseTags(settings.get("include_tags"));
        excludeTags = parseTags(settings.get("exclude_tags"));
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2StemFilter(tokenStream, morph, includeTags, excludeTags);
    }
}
