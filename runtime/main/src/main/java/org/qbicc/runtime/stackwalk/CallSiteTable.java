package org.qbicc.runtime.stackwalk;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.unwind.LibUnwind.*;

import java.lang.invoke.MethodType;

import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.main.CompilerIntrinsics;

/**
 * Method information table for stack walking.
 */
public final class CallSiteTable {
    /**
     * The (sorted) instruction table.
     */
    @extern
    static struct_call_site[] call_site_tbl;

    /**
     * The size of the instruction table.
     */
    @extern
    static uint64_t call_site_tbl_size;

    /**
     * The source code information table.
     */
    @extern
    static struct_source[] source_tbl;

    /**
     * The subprogram information table.
     */
    @extern
    static struct_subprogram[] subprogram_tbl;

    /**
     * The live value information table.
     */
    @extern
    static uint16_t[] lvi_tbl;

    /**
     * The table of file name reference values.
     */
    @extern
    static reference<String>[] file_name_refs;

    /**
     * The table of method name reference values.
     */
    @extern
    static reference<String>[] method_name_refs;

    /**
     * The table of method type reference values.
     */
    @extern
    static reference<MethodType>[] method_type_refs;

    /**
     * An entry in the instruction table.
     */
    @internal
    public static final class struct_call_site extends object {
        /**
         * The instruction pointer for this entry.
         */
        public void_ptr ip;
        /**
         * The index into the source code information table for this entry.
         */
        public uint32_t source_idx;
        /**
         * The index into the live value information table for this entry.
         */
        public uint32_t lvi_idx;
    }

    @internal
    public static final class struct_source extends object {
        public uint32_t subprogram_idx;
        public uint32_t line;
        public uint32_t bci;
        public uint32_t inlined_at_source_idx;
    }

    @internal
    public static final class struct_subprogram extends object {
        public uint32_t file_name_idx;
        public uint32_t method_name_idx;
        public uint32_t method_type_idx;
        public type_id type_id;
        public int modifiers;
    }

    /*  Live value info memory format:
     * ┏━┯━┯━┯━┯━┯━┯━┯━┯━┯━┯━┯━┯━┯━┯━┯━┓
     * ┃F┊E┊D┊C┊B┊A┊9┊8┊7┊6┊5┊4┊3┊2┊1┊0┃ Bit#
     * ┡━┿━┷━┷━┷━┷━┿━┿━┷━┷━┷━┷━┷━┷━┷━┷━┩
     * │1│ BaseReg │±┊  Signed offset  │ Locate in memory; offset is in words (may be 0)
     * ├─┴─┬───────┴─┴─────────────────┤
     * │0 1│     Bits, 1 per word      │ References in memory, offsets are in words
     * ├───┴─┬─┬───────────────────────┤
     * │0 0 1│±┊     Signed offset     │ Add to in-memory offset; offset is in words (cannot be zero; 0 = 1 << 12)
     * ├─────┴─┼───────────────────────┤
     * │0 0 0 1│           -           │ Reserved
     * ├───────┴─┬─────────────────────┤
     * │0 0 0 0 1│          -          │ Reserved
     * ├─────────┴─┬───┬───────────────┤
     * │0 0 0 0 0 1│RHi│ 1 Bit per reg │ In register(s); RHi selects registers in up to 4 banks of 8
     * ├───────────┴─┬─┴───────────────┤
     * │0 0 0 0 0 0 1│        -        │ Reserved
     * ├─────────────┴─┬───────────────┤
     * │0 0 0 0 0 0 0 1│       -       │ Reserved
     * ├───────────────┴─┬─────────────┤
     * │0 0 0 0 0 0 0 0 1│      -      │ Reserved
     * ├─────────────────┴─┬───────────┤
     * │0 0 0 0 0 0 0 0 0 1│     -     │ Reserved
     * ├───────────────────┴─┬─────────┤
     * │0 0 0 0 0 0 0 0 0 0 1│    -    │ Reserved
     * ├─────────────────────┴─┬───────┤
     * │0 0 0 0 0 0 0 0 0 0 0 1│   -   │ Reserved
     * ├───────────────────────┴─┬─────┤
     * │0 0 0 0 0 0 0 0 0 0 0 0 1│  -  │ Reserved
     * ├─────────────────────────┴─┬───┤
     * │0 0 0 0 0 0 0 0 0 0 0 0 0 1│ - │ Reserved
     * ├───────────────────────────┴─┬─┤
     * │0 0 0 0 0 0 0 0 0 0 0 0 0 0 1│-│ Reserved
     * ├─────────────────────────────┴─┤
     * │0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1│ Current memory address is a reference
     * ├───────────────────────────────┤
     * │0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0│ End of list
     * └───────────────────────────────┘
     */

    public static final int LVI_BO_MIN_VALUE = -1 << 9;
    public static final int LVI_BO_MAX_VALUE = (1 << 9) - 1;

    public static final int LVI_AO_MIN_VALUE = -1 << 13;
    public static final int LVI_AO_MAX_VALUE = 1 << 13;

    public static final int LVI_BASE_OFFSET = 0;
    public static final int LVI_IN_MEMORY = 1;
    public static final int LVI_ADD_OFFSET = 2;
    public static final int LVI_RESERVED3 = 3;
    public static final int LVI_RESERVED4 = 4;
    public static final int LVI_IN_REGISTER = 5;
    public static final int LVI_RESERVED6 = 6;
    public static final int LVI_RESERVED7 = 7;
    public static final int LVI_RESERVED8 = 8;
    public static final int LVI_RESERVED9 = 9;
    public static final int LVI_RESERVED10 = 10;
    public static final int LVI_RESERVED11 = 11;
    public static final int LVI_RESERVED12 = 12;
    public static final int LVI_RESERVED13 = 13;
    public static final int LVI_RESERVED14 = 14;
    public static final int LVI_CURRENT_ADDRESS = 15;
    public static final int LVI_END_OF_LIST = 16;

    // * -> struct_instr_tbl

    @NoSafePoint
    @NoThrow
    static ptr<@c_const struct_call_site> findInsnTableEntry(ptr<@c_const ?> ip) {
        final int size = call_site_tbl_size.intValue();

        long ip_int = ip.longValue();
        int low = 0;
        int high = size - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = deref(addr_of(call_site_tbl[mid])).ip.longValue();

            final int cmp = Long.compareUnsigned(midVal, ip_int);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return addr_of(call_site_tbl[mid]);
            }
        }
        // no entry
        return null;
    }

    // struct_call_site -> *

    @NoSafePoint
    @NoThrow
    static ptr<@c_const struct_source> getCallSiteSourceInfo(ptr<@c_const struct_call_site> call_site_ptr) {
        return getSourceInfo(getCallSiteSourceIndex(call_site_ptr));
    }

    @NoSafePoint
    @NoThrow
    static int getCallSiteSourceIndex(ptr<@c_const struct_call_site> call_site_ptr) {
        return deref(call_site_ptr).source_idx.intValue();
    }

    @NoSafePoint
    @NoThrow
    static ptr<@c_const struct_source> getSourceInfo(int index) {
        return addr_of(source_tbl[index]);
    }

    // struct_source -> *

    @NoSafePoint
    @NoThrow
    static ptr<@c_const struct_source> getInlinedAt(ptr<@c_const struct_source> source_entry_ptr) {
        final uint32_t inlinedAt = deref(source_entry_ptr).inlined_at_source_idx;
        if (inlinedAt.intValue() == -1) {
            // not inlined
            return null;
        }
        return addr_of(source_tbl[inlinedAt.intValue()]);
    }

    @NoSafePoint
    @NoThrow
    static ptr<@c_const struct_subprogram> getMethodInfo(ptr<@c_const struct_source> source_entry_ptr) {
        return addr_of(subprogram_tbl[deref(source_entry_ptr).subprogram_idx.intValue()]);
    }

    @NoSafePoint
    @NoThrow
    static int getSourceLine(ptr<@c_const struct_source> source_entry_ptr) {
        return deref(source_entry_ptr).line.intValue();
    }

    // struct_method -> *

    @NoSafePoint
    @NoThrow
    static String getMethodFileName(ptr<@c_const struct_subprogram> method_ptr) {
        final int idx = deref(method_ptr).file_name_idx.intValue();
        return idx == -1 ? null : file_name_refs[idx].toObject();
    }

    @NoSafePoint
    @NoThrow
    static String getMethodName(ptr<@c_const struct_subprogram> method_ptr) {
        return method_name_refs[deref(method_ptr).method_name_idx.intValue()].toObject();
    }

    @NoSafePoint
    @NoThrow
    static MethodType getMethodType(ptr<@c_const struct_subprogram> method_ptr) {
        return method_type_refs[deref(method_ptr).method_type_idx.intValue()].toObject();
    }

    @NoSafePoint
    @NoThrow
    static Class<?> getEnclosingTypeClass(ptr<@c_const struct_subprogram> method_ptr) {
        return CompilerIntrinsics.getClassFromTypeIdSimple(getEnclosingType(method_ptr));
    }

    @NoSafePoint
    @NoThrow
    static type_id getEnclosingType(ptr<@c_const struct_subprogram> method_ptr) {
        return deref(method_ptr).type_id;
    }

    @NoSafePoint
    @NoThrow
    static int getMethodModifiers(ptr<@c_const struct_subprogram> method_ptr) {
        return deref(method_ptr).modifiers;
    }

    @NoSafePoint
    @NoThrow
    static boolean methodHasAllModifiersOf(ptr<@c_const struct_subprogram> method_ptr, int mods) {
        return mods == (mods & deref(method_ptr).modifiers);
    }

    @internal
    public static final class lvi_iterator extends object {
        uint16_t state;
        uint16_t prev_state;
        uintptr_t address;
        ptr<@c_const uint16_t> next_entry;
    }

    /**
     * Initialize a live values bitmap iterator.
     *
     * @param iter the iterator pointer, which typically should be stack-allocated (must not be {@code null})
     * @param instr_ptr the pointer to the corresponding instruction table entry (must not be {@code null})
     */
    @NoSafePoint
    @NoThrow
    @export
    public static void lvi_iterator_init(ptr<lvi_iterator> iter, ptr<@c_const struct_call_site> instr_ptr) {
        deref(iter).address = zero();
        final int lvi_idx = deref(instr_ptr).lvi_idx.intValue();
        final ptr<uint16_t> first_entry = addr_of(lvi_tbl[lvi_idx]);
        deref(iter).prev_state = zero();
        deref(iter).state = deref(first_entry);
        deref(iter).next_entry = first_entry.plus(1);
    }

    /**
     * Retrieve the next reachable reference from the iterator over a live values bitmap.
     *
     * @param iter the iterator pointer, which typically should be stack-allocated (must not be {@code null})
     * @param cursor_ptr the {@code unwind} stack cursor pointer which is used to read registers (must not be {@code null})
     * @return the next non-{@code null} reference, or {@code null} if the end of the bitmap was reached
     */
    @NoSafePoint
    @NoThrow
    @export
    public static reference<?> lvi_iterator_next(ptr<lvi_iterator> iter, ptr<unw_cursor_t> cursor_ptr) {
        final unw_word_t regVal = auto();
        int state = deref(iter).state.intValue();
        ptr<reference<?>> address = deref(iter).address.cast();
        for (;;) {
            switch (Integer.numberOfLeadingZeros(state) - 16) {
                case LVI_BASE_OFFSET -> {
                    int regNum = state >>> 10 & 0x1f;
                    unw_get_reg(cursor_ptr, word(regNum), addr_of(regVal));
                    ptr<reference<?>> refPtr = regVal.cast();
                    int offset = state << 22 >> 22; // sign-extend and drop high bits
                    address = refPtr.plus(offset).cast();
                }
                case LVI_IN_MEMORY -> {
                    while (state != 0b01_00_0000_0000_0000) {
                        // the lowest bit locates the reference
                        final int lob = Integer.lowestOneBit(state);
                        state &= ~lob;
                        final int offset = Integer.numberOfTrailingZeros(lob);
                        final reference<?> ref = address.plus(offset).loadUnshared();
                        if (ref != null) {
                            // save previous state
                            deref(iter).prev_state = word(state | lob);
                            // save state for next iteration
                            deref(iter).state = word(state);
                            deref(iter).address = address.cast();
                            return ref;
                        }
                        // null reference; continue on to the next one
                    }
                    // else move on to next word, 14 slots later
                    address = address.plus(14).cast();
                }
                case LVI_ADD_OFFSET -> {
                    // add an offset to the current address
                    int offset = (short)(state << 3) >> 3; // sign-extend
                    if (offset == 0) offset = LVI_AO_MAX_VALUE; // zero offset is nonsensical; reuse the bit pattern
                    address = address.plus(offset).cast();
                }
                case LVI_IN_REGISTER -> {
                    // bits 8 and 9 contain the bank of 8 registers, so multiply that by 8 for the register base
                    int regBase = state >> 5 & 0b11000; // (item >> 8 & 0b11) << 3
                    while ((state & 0b1111_1111) != 0) {
                        final int lob = Integer.lowestOneBit(state);
                        state ^= lob;
                        final int idx = Integer.numberOfTrailingZeros(lob);
                        unw_get_reg(cursor_ptr, word(regBase + idx), addr_of(regVal));
                        final reference<?> ref = reference.fromWord(regVal);
                        if (ref != null) {
                            // save previous state
                            deref(iter).prev_state = word(state | lob);
                            // save state for next iteration
                            deref(iter).state = word(state);
                            deref(iter).address = address.cast();
                            return ref;
                        }
                    }
                    // else move on to next word (address is unaffected since the refs are in registers)
                }
                case LVI_CURRENT_ADDRESS -> {
                    // the current address value should be translated into a reference (stack allocated object)
                    deref(iter).state = deref(deref(iter).next_entry);
                    deref(iter).address = address.cast();
                    // move forward to next item
                    deref(iter).next_entry = deref(iter).next_entry.plus(1);
                    return reference.of(ptrToRef(address.cast()));
                }
                case LVI_END_OF_LIST -> {
                    deref(iter).state = zero();
                    // done!
                    return null;
                }
            }
            state = deref(iter).next_entry.loadUnshared().intValue();
            deref(iter).next_entry = deref(iter).next_entry.plus(1);
        }
    }

    /**
     * Modify the reference value last returned from the iterator.
     *
     * @param iter the iterator pointer, which typically should be stack-allocated (must not be {@code null})
     * @param cursor_ptr the {@code unwind} stack cursor pointer which is used to read registers (must not be {@code null})
     * @param val the new reference value to write
     * @throws IllegalStateException if the state of the iterator is not valid
     */
    @NoSafePoint
    @NoThrow
    @export
    public static void lvi_iterator_set(ptr<lvi_iterator> iter, ptr<unw_cursor_t> cursor_ptr, reference<?> val) {
        int state = deref(iter).prev_state.intValue();
        ptr<reference<?>> address = deref(iter).address.cast();
        switch (Integer.numberOfLeadingZeros(state) - 16) {
            default -> abort();
            case LVI_IN_MEMORY -> address.plus(Integer.numberOfTrailingZeros(Integer.lowestOneBit(state))).storeUnshared(val);
            case LVI_IN_REGISTER -> {
                // bits 8 and 9 contain the bank of 8 registers, so multiply that by 8 for the register base
                int regBase = state >> 5 & 0b11000; // (item >> 8 & 0b11) << 3
                final int idx = Integer.numberOfTrailingZeros(Integer.lowestOneBit(state));
                unw_set_reg(cursor_ptr, word(regBase + idx), val.cast());
            }
        }
    }
}
