package net.uaprom.jmorphy2;

import java.io.File;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.ArrayList;

import org.apache.commons.io.input.SwappedDataInputStream;
import org.apache.commons.codec.binary.Base64;


public class DAWG {
    private static final byte PAYLOAD_SEPARATOR = 0x01;

    private DAWGDict dict;
    private Guide guide;

    public DAWG(InputStream stream) throws IOException {
        DataInput input = new SwappedDataInputStream(new BufferedInputStream(stream));
        dict = new DAWGDict(input);
        guide = new Guide(input);
    }

    public DAWG(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public ArrayList<FoundParadigm> valueForIndex(int index, String key) throws IOException {
        ArrayList<FoundParadigm> values = new ArrayList<FoundParadigm>();

        Completer completer = new Completer(dict, guide);
        
        completer.start(index);
        while (completer.next()) {
            byte[] encodedData = completer.getKey();
            Base64 base64 = new Base64();
            byte[] decodedData = base64.decodeBase64(encodedData);

            DataInput stream = new DataInputStream(new ByteArrayInputStream(decodedData));
            short paradigmId = stream.readShort();
            short idx = stream.readShort();
            values.add(new FoundParadigm(paradigmId, idx, key));
        }

        return values;
    }

    public ArrayList<FoundParadigm> similarItems(String key) throws IOException {
        return similarItems(key, null);
    }

    public ArrayList<FoundParadigm> similarItems(String key, Map<Character,String> replaceChars) throws IOException {
        return similarItems(key, replaceChars, "", 0);
    }

    private ArrayList<FoundParadigm> similarItems(String key, Map<Character,String> replaceChars, String prefix, int index) throws IOException {
        ArrayList<FoundParadigm> foundParadigms = new ArrayList<FoundParadigm>();
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
                            foundParadigms.addAll(similarItems(key, replaceChars, nextPrefix, nextIndex));
                        }
                    }
                }
            }

            index = dict.followBytes(Character.toString(c).getBytes("UTF-8"), index);
            if (index == -1) {
                return foundParadigms;
            }
        }
        
        if (index == -1) {
            return foundParadigms;
        }

        index = dict.followByte(PAYLOAD_SEPARATOR, index);
        if (index == -1) {
            return foundParadigms;
        }

        return valueForIndex(index, prefix + key.substring(prefixLength));
    }

    private class DAWGDict {
        private static final int PRECISION_MASK = 0xFFFFFFFF;

        private static final int HAS_LEAF_BIT = 1 << 8;
        private static final int EXTENSION_BIT = 1 << 9;
        private static final int OFFSET_MAX = 1 << 21;
        private static final int IS_LEAF_BIT = 1 << 31;
    
        // private Units units;
        private int[] units;

        public DAWGDict(DataInput input) throws IOException {
            int size = input.readInt();
            units = new int[size];
            for (int i = 0; i < size; i++) {
                units[i] = input.readInt();
            }
        }

        public boolean contains(byte[] key) {
            int index = followBytes(key, 0);
            if (index == -1) {
                return false;
            }
        
            return hasValue(index);
        }

        public int followBytes(byte[] key, int index) {
            for (int i = 0; i < key.length; i++) {
                index = followByte(key[i], index);
                if (index == -1) {
                    return -1;
                }
            }

            return index;
        }
    
        public int followByte(byte c, int index) {
            int o = offset(units[index]);
            int nextIndex = (index ^ o ^ (c & 0xFF)) & PRECISION_MASK;

            if (label(units[nextIndex]) != (c & 0xFF)) {
                return -1;
            }

            return nextIndex;
        }

        public boolean hasValue(int index) {
            return hasLeaf(units[index]);
        }

        protected int offset(int base) {
            return ((base >> 10) << ((base & EXTENSION_BIT) >> 6)) & PRECISION_MASK;
        }

        protected int label(int base) {
            return base & (IS_LEAF_BIT | 0xFF) & PRECISION_MASK;
        }

        protected int value(int base) {
            return base & ~IS_LEAF_BIT & PRECISION_MASK;
        }

        protected boolean hasLeaf(int base) {
            return (base & HAS_LEAF_BIT & PRECISION_MASK) != 0;
        }
    };

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

    public static class Paradigm {
        public short[] paradigm;

        public Paradigm(DataInput input) throws IOException {
            short length = input.readShort();
            paradigm = new short[length];
            for (int i = 0; i < length; i++) {
                paradigm[i] = input.readShort();
            }
        }

        public short[] getParadigm() {
            return paradigm;
        }
    };

    public class FoundParadigm {
        public short paradigmId;
        public short idx;
        public String key;

        public FoundParadigm(short paradigmId, short idx, String key) {
            this.paradigmId = paradigmId;
            this.idx = idx;
            this.key = key;
        }
    };

    // private class Units {
    //     private int size;
    //     private int[] units;

    //     public Units(DataInput input) {
    //     }
    // }
}
