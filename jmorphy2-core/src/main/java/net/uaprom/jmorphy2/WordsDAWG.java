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
        DataInput stream = new DataInputStream(new ByteArrayInputStream(decodeValue(value)));
        short paraId = stream.readShort();
        short idx = stream.readShort();
        return new FoundParadigm(key, paraId, idx);
    }

    public static class FoundParadigm extends PayloadsDAWG.Payload {
        public final short paraId;
        public final short idx;

        public FoundParadigm(String key, short paraId, short idx) {
            super(key);
            this.paraId = paraId;
            this.idx = idx;
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass()) {
                return false;
            }

            FoundParadigm other = (FoundParadigm) obj;
            return key.equals(other.key)
                && paraId == other.paraId
                && idx == other.idx;
        }
    };
}
