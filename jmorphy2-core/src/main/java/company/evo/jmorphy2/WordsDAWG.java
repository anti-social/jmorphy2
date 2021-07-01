package company.evo.jmorphy2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import company.evo.dawg.PayloadsDAWG;


public class WordsDAWG extends PayloadsDAWG {
    public WordsDAWG(InputStream stream) throws IOException {
        super(stream);
    }

    protected WordForm decodePayload(Payload payload) {
        ByteBuffer data = ByteBuffer.wrap(payload.value);
        short paradigmId = data.getShort();
        short idx = data.getShort();
        return new WordForm(payload.key, paradigmId, idx);
    }

    public List<WordForm> similarWords(String word, Map<Character,String> replaceChars) {
        List<WordForm> foundWords = new ArrayList<>();
        for (Payload payload : similarItems(word, replaceChars)) {
            foundWords.add(decodePayload(payload));
        }
        return foundWords;
    }

    public static class WordForm {
        public final String word;
        public final short paradigmId;
        public final short idx;

        public WordForm(String word, short paradigmId, short idx) {
            this.word = word;
            this.paradigmId = paradigmId;
            this.idx = idx;
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass()) {
                return false;
            }

            WordForm other = (WordForm) obj;
            return word.equals(other.word)
                && paradigmId == other.paradigmId
                && idx == other.idx;
        }
    };
}
