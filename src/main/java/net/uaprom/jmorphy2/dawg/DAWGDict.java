package net.uaprom.jmorphy2.dawg;

import java.io.DataInput;
import java.io.IOException;


public class DAWGDict {
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
        int index = followBytes(key);
        if (index == -1) {
            return false;
        }
        
        return hasValue(index);
    }

    public int find(byte[] key) {
        int index = followBytes(key);
        if (index == -1) {
            return -1;
        }
        if (!hasValue(index)) {
            return -1;
        }
        return value(index);
    }

    public int followBytes(byte[] key) {
        return followBytes(key, 0);
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
}
