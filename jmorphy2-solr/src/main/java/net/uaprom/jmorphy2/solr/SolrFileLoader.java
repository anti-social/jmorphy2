package net.uaprom.jmorphy2.solr;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;

import org.apache.lucene.analysis.util.ResourceLoader;

import net.uaprom.jmorphy2.FileLoader;


public class SolrFileLoader extends FileLoader {
    private final ResourceLoader loader;
    private final String basePath;

    public SolrFileLoader(ResourceLoader loader, String basePath) {
        this.loader = loader;
        this.basePath = basePath;
    }

    @Override
    public InputStream getStream(String filename) throws IOException {
        return loader.openResource((new File(basePath, filename)).getPath());
    }
}
