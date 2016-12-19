package company.evo.dawg;

import java.io.InputStream;
import java.io.IOException;


public class IntegerDAWG extends DAWG {
    public IntegerDAWG(InputStream stream) throws IOException {
        super(stream);
    }

    public Integer get(String key) throws IOException {
        return get(key, null);
    }

    public Integer get(String key, Integer defaultValue) throws IOException {
        int res = dict.find(key.getBytes("UTF-8"));
        if (res == Dict.MISSING) {
            return defaultValue;
        }
        return res;
    }
}
