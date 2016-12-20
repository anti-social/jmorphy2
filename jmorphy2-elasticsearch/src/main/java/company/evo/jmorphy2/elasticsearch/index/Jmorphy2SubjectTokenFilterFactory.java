package company.evo.jmorphy2.elasticsearch.index;

import java.util.Set;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.nlp.SubjectExtractor;
import company.evo.jmorphy2.lucene.Jmorphy2SubjectFilter;
import company.evo.jmorphy2.elasticsearch.indices.Jmorphy2Analysis;


public class Jmorphy2SubjectTokenFilterFactory extends AbstractTokenFilterFactory {
    private final SubjectExtractor subjExtractor;
    private final int maxSentenceLength;

    public Jmorphy2SubjectTokenFilterFactory(IndexSettings indexSettings,
                                             Environment environment,
                                             String name,
                                             Settings settings) {
        super(indexSettings, name, settings);

        // subjExtractor = getSubjectExtractor(dictName);
        subjExtractor = null;

        maxSentenceLength = settings.getAsInt("max_sentence_length", 10);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new Jmorphy2SubjectFilter(tokenStream, subjExtractor, maxSentenceLength);
    }

    // private SubjectExtractor getSubjectExtractor(String name, Settings settings, Environment env)
    //     throws IOException
    // {
    //     String dictName = settings.get("name");
    //     MorphAnalyzer morph = getMorphAnalyzer(name);
    //     File dicDir = new File(jmorphy2Dir, name);
    //     File taggerRulesFile = new File(dicDir, "tagger_rules.txt");
    //     Tagger tagger =
    //         new SimpleTagger(morph,
    //                          new Ruleset(new FileInputStream(taggerRulesFile)),
    //                          settings.getAsInt("tagger_threshold", SimpleTagger.DEFAULT_THRESHOLD));
    //     File parserRulesFile = new File(dicDir, "parser_rules.txt");
    //     Parser parser =
    //         new SimpleParser(morph,
    //                          tagger,
    //                          new Ruleset(new FileInputStream(parserRulesFile)),
    //                          settings.getAsInt("parser_threshold", SimpleParser.DEFAULT_THRESHOLD));

    //     Path extractionRulesPath = Paths.get(dicDir.getPath(), "extract_rules.txt");
    //     String extractionRules = new String(Files.readAllBytes(extractionRulesPath));
    //     return new SubjectExtractor(parser, extractionRules, true);
    // }
}
