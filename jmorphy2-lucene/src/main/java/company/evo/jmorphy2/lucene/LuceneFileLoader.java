package company.evo.jmorphy2.lucene;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;

import org.apache.lucene.analysis.util.ResourceLoader;

import company.evo.jmorphy2.FileLoader;


public class LuceneFileLoader extends FileLoader {
    private final ResourceLoader loader;
    private final String basePath;

    public LuceneFileLoader(ResourceLoader loader, String basePath) {
        this.loader = loader;
        this.basePath = basePath;
    }

    @Override
    public InputStream newStream(String filename) throws IOException {
        return loader.openResource((new File(basePath, filename)).getPath());
    }
}
