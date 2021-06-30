package company.evo.dawg;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class IntegerDAWG extends DAWG {
    public IntegerDAWG(InputStream stream) throws IOException {
        super(stream);
    }

    public Integer get(String key) {
        return get(key, null);
    }

    public Integer get(String key, Integer defaultValue) {
        int res = dict.find(key.getBytes(StandardCharsets.UTF_8));
        if (res == Dict.MISSING) {
            return defaultValue;
        }
        return res;
    }
}
