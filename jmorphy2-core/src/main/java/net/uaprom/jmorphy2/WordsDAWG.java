package net.uaprom.jmorphy2;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.uaprom.dawg.PayloadsDAWG;


public class WordsDAWG extends PayloadsDAWG {
    public WordsDAWG(InputStream stream) throws IOException {
        super(stream);
    }

    @Override
    protected Payload newPayload(String key, byte[] value) throws IOException {
        return new FoundParadigm(key, value);
    }

    public class FoundParadigm extends PayloadsDAWG.Payload {
        public final short paraId;
        public final short idx;

        public FoundParadigm(String key, byte[] value) throws IOException {
            super(key, value);

            DataInput stream = new DataInputStream(new ByteArrayInputStream(this.value));
            this.paraId = stream.readShort();
            this.idx = stream.readShort();
        }
    };
}
