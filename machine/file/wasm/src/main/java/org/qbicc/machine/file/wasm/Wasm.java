package org.qbicc.machine.file.wasm;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class Wasm {
    public static final long LIMITS_MAXIMUM = 0x1_0000_0000L;

    private Wasm() {}

    public enum Section {
        // order is significant!
        CUSTOM(SECTION_CUSTOM),
        TYPE(SECTION_TYPE),
        IMPORT(SECTION_IMPORT),
        FUNCTION(SECTION_FUNCTION),
        TABLE(SECTION_TABLE),
        MEMORY(SECTION_MEMORY),
        TAG(SECTION_TAG),
        GLOBAL(SECTION_GLOBAL),
        EXPORT(SECTION_EXPORT),
        START(SECTION_START),
        ELEMENT(SECTION_ELEMENT),
        DATA_COUNT(SECTION_DATA_COUNT),
        CODE(SECTION_CODE),
        DATA(SECTION_DATA),
        ;

        private static final Section[] vals = values();
        private final int id;

        Section(final int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public boolean succeeds(Section other) {
            return ordinal() > other.ordinal();
        }

        public boolean precedes(Section other) {
            return ordinal() < other.ordinal();
        }

        public static Section forId(int id) {
            Assert.checkMinimumParameter("id", 0, id);
            Assert.checkMaximumParameter("id", vals.length - 1, id);
            return switch (id) {
                case SECTION_CUSTOM -> CUSTOM;
                case SECTION_TYPE -> TYPE;
                case SECTION_IMPORT -> IMPORT;
                case SECTION_FUNCTION -> FUNCTION;
                case SECTION_TABLE -> TABLE;
                case SECTION_MEMORY -> MEMORY;
                case SECTION_GLOBAL -> GLOBAL;
                case SECTION_EXPORT -> EXPORT;
                case SECTION_START -> START;
                case SECTION_ELEMENT -> ELEMENT;
                case SECTION_CODE -> CODE;
                case SECTION_DATA -> DATA;
                case SECTION_DATA_COUNT -> DATA_COUNT;
                case SECTION_TAG -> TAG;
                default -> throw Assert.impossibleSwitchCase(id);
            };
        }
    }

    public static final int SECTION_CUSTOM = 0;
    public static final int SECTION_TYPE = 1;
    public static final int SECTION_IMPORT = 2;
    public static final int SECTION_FUNCTION = 3;
    public static final int SECTION_TABLE = 4;
    public static final int SECTION_MEMORY = 5;
    public static final int SECTION_GLOBAL = 6;
    public static final int SECTION_EXPORT = 7;
    public static final int SECTION_START = 8;
    public static final int SECTION_ELEMENT = 9;
    public static final int SECTION_CODE = 10;
    public static final int SECTION_DATA = 11;
    public static final int SECTION_DATA_COUNT = 12;
    public static final int SECTION_TAG = 13;

    /**
     * Compute the exact size of a uleb128 value.
     *
     * @param val the value
     * @return the size
     */
    public static int uleb128_size(int val) {
        if (val == 0) {
            return 1;
        }
        int hob = Integer.highestOneBit(val);
        int ntz = Integer.numberOfTrailingZeros(hob);
        return (ntz / 7) + 1;
    }
}
