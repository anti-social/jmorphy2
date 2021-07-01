package company.evo.jmorphy2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import company.evo.dawg.PayloadsDAWG;


public class SuffixesDAWG extends PayloadsDAWG {
    public SuffixesDAWG(InputStream stream) throws IOException {
        super(stream);
    }

    protected SuffixForm decodePayload(Payload payload) {
        ByteBuffer data = ByteBuffer.wrap(payload.value);
        short count = data.getShort();
        short paradigmId = data.getShort();
        short idx = data.getShort();
        return new SuffixForm(payload.key, count, paradigmId, idx);
    }

    public List<SuffixForm> similarSuffixes(String word, Map<Character,String> replaceChars) {
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
