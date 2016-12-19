package company.evo.dawg;

import java.io.DataInput;
import java.io.IOException;


class Dict {
    public static final int ROOT = 0;
    public static final int MISSING = -1;

    private int[] units;

    public Dict(DataInput input) throws IOException {
        int size = input.readInt();
        units = new int[size];
        for (int i = 0; i < size; i++) {
            units[i] = input.readInt();
        }
    }

    public boolean contains(byte[] key) {
        int index = followBytes(key);
        if (index == MISSING) {
            return false;
        }

        return hasValue(index);
    }

    public int find(byte[] key) {
        int index = followBytes(key);
        if (index == MISSING) {
            return MISSING;
        }
        if (!hasValue(index)) {
            return MISSING;
        }
        return value(index);
    }

    public int followBytes(byte[] key) {
        return followBytes(key, ROOT);
    }

    public int followBytes(byte[] key, int index) {
        for (int i = 0; i < key.length; i++) {
            index = followByte(key[i], index);
            if (index == MISSING) {
                return MISSING;
            }
        }

        return index;
    }

    public int followByte(byte c, int index) {
        int o = Units.offset(units[index]);
        int nextIndex = (index ^ o ^ (c & 0xFF)) & Units.PRECISION_MASK;

        if (Units.label(units[nextIndex]) != (c & 0xFF)) {
            return MISSING;
        }

        return nextIndex;
    }

    public boolean hasValue(int index) {
        return Units.hasLeaf(units[index]);
    }

    public int value(int index) {
        int o = Units.offset(units[index]);
        int valueIndex = (index ^ o) & Units.PRECISION_MASK;
        return Units.value(units[valueIndex]);
    }

    static class Units {
        public static final int PRECISION_MASK = 0xFFFFFFFF;

        public static final int HAS_LEAF_BIT = 1 << 8;
        public static final int EXTENSION_BIT = 1 << 9;
        public static final int OFFSET_MAX = 1 << 21;
        public static final int IS_LEAF_BIT = 1 << 31;

        public static int value(int base) {
            return base & ~IS_LEAF_BIT & PRECISION_MASK;
        }

        public static int offset(int base) {
            return ((base >> 10) << ((base & EXTENSION_BIT) >> 6)) & PRECISION_MASK;
        }

        public static int label(int base) {
            return base & (IS_LEAF_BIT | 0xFF) & PRECISION_MASK;
        }

        public static boolean hasLeaf(int base) {
            return (base & HAS_LEAF_BIT & PRECISION_MASK) != 0;
        }
    };
}
