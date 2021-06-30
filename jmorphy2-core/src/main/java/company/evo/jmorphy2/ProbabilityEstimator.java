package company.evo.jmorphy2;

import java.io.InputStream;
import java.io.IOException;

import company.evo.dawg.IntegerDAWG;


public class ProbabilityEstimator {
    public static final String PROBABILITY_FILENAME = "p_t_given_w.intdawg";
    public static final String KEY_FORMAT = "%s:%s";
    public static final float MULTIPLIER = 1000000f;

    private IntegerDAWG dict;

    public ProbabilityEstimator(FileLoader loader) throws IOException {
        InputStream stream = loader.newStream(PROBABILITY_FILENAME);
        dict = new IntegerDAWG(stream);
        stream.close();
    }

    public float getProbability(String word, Tag tag) {
        String key = String.format(KEY_FORMAT, word, tag);
        return dict.get(key, 0) / MULTIPLIER;
    }
}
