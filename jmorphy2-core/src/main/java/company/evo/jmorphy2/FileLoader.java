package company.evo.jmorphy2;

import java.io.IOException;
import java.io.InputStream;


public abstract class FileLoader {
    public abstract InputStream getStream(String filename) throws IOException;
}
