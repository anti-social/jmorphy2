package net.uaprom.jmorphy2.dawg;

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
        if (res == -1) {
            return defaultValue;
        }
        return res;
    }
}
