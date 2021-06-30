package company.evo.dawg;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;


public class PayloadsDAWG extends DAWG {
    private static final byte PAYLOAD_SEPARATOR = 0x01;

    private final Guide guide;

    public PayloadsDAWG(InputStream stream) throws IOException {
        super(stream);
        guide = new Guide(input);
    }

    protected List<Payload> valueForIndex(int index, String key) {
        List<Payload> values = new ArrayList<Payload>();

        Completer completer = new Completer(dict, guide);

        int i = 0;
        completer.start(index);
        while (completer.next()) {
            values.add(new Payload(key, decodeValue(completer.getKey())));
        }

        return values;
    }

    protected byte[] decodeValue(byte[] value) {
        return Base64.decodeBase64(value);
    }

    public List<Payload> similarItems(String key) {
        return similarItems(key, null);
    }

    public List<Payload> similarItems(String key, Map<Character,String> replaceChars) {
        return similarItems(key, replaceChars, "", Dict.ROOT);
    }

    private List<Payload> similarItems(String key,
                                       Map<Character,String> replaceChars,
                                       String prefix,
                                       int index)
    {
        List<Payload> items = new ArrayList<Payload>();
        int keyLength = key.length();
        int prefixLength = prefix.length();

        for (int i = prefixLength; i < keyLength; i++) {
            char c = key.charAt(i);

            if (replaceChars != null) {
                String replaces = replaceChars.get(c);
                if (replaces != null) {
                    int replacesLength = replaces.length();
                    for (int j = 0; j < replacesLength; j++) {
                        String r = replaces.substring(j, j + 1);
                        int nextIndex = dict.followBytes(r.getBytes(StandardCharsets.UTF_8), index);
                        if (nextIndex != Dict.MISSING) {
                            String nextPrefix = prefix + key.substring(prefixLength, i) + r;
                            items.addAll(similarItems(key, replaceChars, nextPrefix, nextIndex));
                        }
                    }
                }
            }

            index = dict.followBytes(Character.toString(c).getBytes(StandardCharsets.UTF_8), index);
            if (index == Dict.MISSING) {
                return items;
            }
        }

        if (index == Dict.MISSING) {
            return items;
        }

        index = dict.followByte(PAYLOAD_SEPARATOR, index);
        if (index == Dict.MISSING) {
            return items;
        }

        return valueForIndex(index, prefix + key.substring(prefixLength));
    }

    private class Guide {
        private byte[] units;

        public Guide(DataInput input) throws IOException {
            int baseSize = input.readInt();

            int size = baseSize * 2;
            units = new byte[size];
            for (int i = 0; i < size; i++) {
                units[i] = input.readByte();
            }
        }

        public byte child(int index) {
            return units[index * 2];
        }

        public byte sibling(int index) {
            return units[index * 2 + 1];
        }

        public int size() {
            return units.length;
        }
    };

    private class Completer {
        private Dict dict;
        private Guide guide;

        private byte[] key;
        private int keyLength;
        private ArrayList<Integer> indexStack;
        private int lastIndex;

        private static final int INITIAL_KEY_LENGTH = 9;

        public Completer(Dict dict, Guide guide) {
            this.dict = dict;
            this.guide = guide;
        }

        public void start(int index) {
            key = new byte[INITIAL_KEY_LENGTH];
            keyLength = 0;
            indexStack = new ArrayList<Integer>();
            indexStack.add(index);
            lastIndex = 0;
        }

        public boolean next() {
            int index = indexStack.get(indexStack.size() - 1);

            if (lastIndex != 0) {
                byte childLabel = guide.child(index);

                if (childLabel != 0) {
                    index = follow(childLabel, index);
                    if (index == Dict.MISSING) {
                        return false;
                    }
                }
                else {
                    while (true) {
                        byte siblingLabel = guide.sibling(index);

                        if (keyLength > 0) {
                            keyLength--;
                        }

                        indexStack.remove(indexStack.size() - 1);
                        if (indexStack.isEmpty()) {
                            return false;
                        }

                        index = indexStack.get(indexStack.size() - 1);
                        if (siblingLabel != 0) {
                            index = follow(siblingLabel, index);
                            if (index == Dict.MISSING) {
                                return false;
                            }

                            break;
                        }
                    }
                }
            }

            return findTerminal(index);
        }

        public byte[] getKey() {
            byte[] newKey = new byte[keyLength];
            System.arraycopy(key, 0, newKey, 0, keyLength);
            return newKey;
        }

        private int follow(byte label, int index) {
            int nextIndex = dict.followByte(label, index);
            if (nextIndex == Dict.MISSING) {
                return Dict.MISSING;
            }

            addLabel(label);
            indexStack.add(nextIndex);
            return nextIndex;
        }

        private boolean findTerminal(int index) {
            while (!dict.hasValue(index)) {
                byte label = guide.child(index);

                index = dict.followByte(label, index);
                if (index == Dict.MISSING) {
                    return false;
                }

                addLabel(label);
                indexStack.add(index);
            }

            lastIndex = index;
            return true;
        }

        private void addLabel(byte label) {
            if (keyLength == key.length) {
                byte[] newKey = new byte[key.length * 2];
                System.arraycopy(key, 0, newKey, 0, key.length);
                key = newKey;
            }

            key[keyLength] = label;
            keyLength++;
        }
    };

    public static class Payload {
        public final String key;
        public final byte[] value;

        public Payload(String key, byte[] value) {
            this.key = key;
            this.value = value;
        }
    };
}
