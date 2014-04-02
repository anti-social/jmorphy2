package net.uaprom.dawg;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;

import org.apache.commons.io.input.SwappedDataInputStream;


public class DAWG {
    protected final DataInput input;
    protected final Dict dict;

    public DAWG(InputStream stream) throws IOException {
        input = new SwappedDataInputStream(new BufferedInputStream(stream));
        dict = new Dict(input);
    }
}
