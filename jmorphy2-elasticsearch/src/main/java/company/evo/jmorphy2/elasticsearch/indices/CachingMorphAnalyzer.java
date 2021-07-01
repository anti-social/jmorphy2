package company.evo.jmorphy2.elasticsearch.indices;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import company.evo.jmorphy2.MorphAnalyzer;
import company.evo.jmorphy2.ParsedWord;
import company.evo.jmorphy2.ProbabilityEstimator;
import company.evo.jmorphy2.Tag;
import company.evo.jmorphy2.units.AnalyzerUnit;

import org.elasticsearch.SpecialPermission;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

public class CachingMorphAnalyzer extends MorphAnalyzer {
    private final LoadingCache<String, List<ParsedWord>> cache;

    public static class Builder extends MorphAnalyzer.Builder<Builder> {
        private int cacheSize;

        public Builder cacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
            return self();
        }

        @Override
        public MorphAnalyzer build() throws IOException {
            if (cacheSize > 0) {
                var prepared = super.prepare();
                return new CachingMorphAnalyzer(
                    tagStorage, prepared.units, prepared.probabilityEstimator, cacheSize
                );
            }
            return super.build();
        }
    }

    private CachingMorphAnalyzer(
        Tag.Storage tagStorage,
        List<AnalyzerUnit> units,
        ProbabilityEstimator prob,
        int cacheSize
    ) {
        super(tagStorage, units, prob);

        SpecialPermission.check();
        cache = AccessController.doPrivileged(
            (PrivilegedAction<LoadingCache<String, List<ParsedWord>>>) () ->
                Caffeine.newBuilder()
                    .maximumSize(cacheSize)
                    .build(super::parse)
        );
    }

    @Override
    public List<ParsedWord> parse(String word) {
        return cache.get(word);
    }
}
