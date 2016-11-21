package net.uaprom.jmorphy2;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.uaprom.dawg.PayloadsDAWG;


public class SuffixesDAWG extends PayloadsDAWG {
    public SuffixesDAWG(InputStream stream) throws IOException {
        super(stream);
    }

    protected SuffixForm decodePayload(Payload payload) throws IOException {
        DataInput stream = new DataInputStream(new ByteArrayInputStream(payload.value));
        short count = stream.readShort();
        short paradigmId = stream.readShort();
        short idx = stream.readShort();
        return new SuffixForm(payload.key, count, paradigmId, idx);
    }

    public List<SuffixForm> similarSuffixes(String word, Map<Character,String> replaceChars) throws IOException {
        List<SuffixForm> foundSuffixes = new ArrayList<>();
        for (Payload payload : similarItems(word, replaceChars)) {
            foundSuffixes.add(decodePayload(payload));
        }
        return foundSuffixes;
    }

    public static class SuffixForm {
        public final String word;
        public final short count;
        public final short paradigmId;
        public final short idx;

        public SuffixForm(String word, short count, short paradigmId, short idx) {
            this.count = count;
            this.word = word;
            this.paradigmId = paradigmId;
            this.idx = idx;
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass()) {
                return false;
            }

            SuffixForm other = (SuffixForm) obj;
            return word.equals(other.word)
                && count == other.count
                && paradigmId == other.paradigmId
                && idx == other.idx;
        }
    };
}
