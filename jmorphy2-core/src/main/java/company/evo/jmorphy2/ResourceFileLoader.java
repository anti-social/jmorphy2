package company.evo.jmorphy2;

import java.io.InputStream;


public class ResourceFileLoader extends FileLoader {
    private final String basePath;

    public ResourceFileLoader(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public InputStream newStream(String filename) {
        return getClass().getResourceAsStream(basePath + "/" + filename);
    }
}
