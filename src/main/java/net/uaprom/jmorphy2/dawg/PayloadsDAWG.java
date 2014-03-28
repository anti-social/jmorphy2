package net.uaprom.jmorphy2.dawg;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
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

    protected List<Payload> valueForIndex(int index, String key) throws IOException {
        List<Payload> values = new ArrayList<Payload>();

        Completer completer = new Completer(dict, guide);

        int i = 0;
        completer.start(index);
        while (completer.next()) {
            values.add(newPayload(key, completer.getKey()));
        }

        return values;
    }

    protected Payload newPayload(String key, byte[] value) throws IOException {
        return new Payload(key, value);
    }

    public List<Payload> similarItems(String key) throws IOException {
        return similarItems(key, null);
    }

    public List<Payload> similarItems(String key, Map<Character,String> replaceChars) throws IOException {
        return similarItems(key, replaceChars, "", 0);
    }

    private List<Payload> similarItems(String key, Map<Character,String> replaceChars, String prefix, int index) throws IOException {
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
                        int nextIndex = dict.followBytes(r.getBytes("UTF-8"), index);
                        if (nextIndex != -1) {
                            String nextPrefix = prefix + key.substring(prefixLength, i) + r;
                            items.addAll(similarItems(key, replaceChars, nextPrefix, nextIndex));
                        }
                    }
                }
            }

            index = dict.followBytes(Character.toString(c).getBytes("UTF-8"), index);
            if (index == -1) {
                return items;
            }
        }
        
        if (index == -1) {
            return items;
        }

        index = dict.followByte(PAYLOAD_SEPARATOR, index);
        if (index == -1) {
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
        private DAWGDict dict;
        private Guide guide;

        private byte[] key;
        private int keyLength;
        private ArrayList<Integer> indexStack;
        private int lastIndex;

        private static final int INITIAL_KEY_LENGTH = 9;

        public Completer(DAWGDict dict, Guide guide) {
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
                    if (index == -1) {
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
                            if (index == -1) {
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
            if (nextIndex == -1) {
                return -1;
            }

            addLabel(label);
            indexStack.add(nextIndex);
            return nextIndex;
        }

        private boolean findTerminal(int index) {
            while (!dict.hasValue(index)) {
                byte label = guide.child(index);

                index = dict.followByte(label, index);
                if (index == -1) {
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

    public class Payload {
        public final String key;
        public final byte[] value;

        public Payload(String key, byte[] value) throws IOException {
            this.key = key;
            this.value = (new Base64()).decodeBase64(value);
        }
    };

    // private class Units {
    //     private int size;
    //     private int[] units;

    //     public Units(DataInput input) {
    //     }
    // }
}
