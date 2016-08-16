package net.uaprom.jmorphy2;

import java.io.IOException;
import java.io.InputStream;


public class ResourceFileLoader extends FileLoader {
    private final String basePath;

    public ResourceFileLoader(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public InputStream getStream(String filename) throws IOException {
        return getClass().getResourceAsStream(basePath + "/" + filename);
    }
}
