package org.qbicc.runtime.methoddata;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

public final class MethodData {
    @internal
    public static final class method_info extends object {
        public uint32_t fileNameIndex;
        public uint32_t classNameIndex;
        public uint32_t methodNameIndex;
        public uint32_t methodDescIndex;
    }

    public static final class method_info_ptr extends ptr<method_info> {}
    public static final class const_method_info_ptr extends ptr<@c_const uint32_t> {}
    public static final class method_info_ptr_ptr extends ptr<method_info_ptr> {}
    public static final class const_method_info_ptr_ptr extends ptr<const_method_info_ptr> {}
    public static final class method_info_ptr_const_ptr extends ptr<@c_const method_info_ptr> {}
    public static final class const_method_info_ptr_const_ptr extends ptr<@c_const const_method_info_ptr> {}

    @extern
    public static method_info[] qbicc_method_info_table;

    public static int getFileNameIndex(int minfoIndex) {
        return qbicc_method_info_table[minfoIndex].fileNameIndex.intValue();
    }

    public static int getClassNameIndex(int minfoIndex) {
        return qbicc_method_info_table[minfoIndex].classNameIndex.intValue();
    }

    public static int getMethodNameIndex(int minfoIndex) {
        return qbicc_method_info_table[minfoIndex].methodNameIndex.intValue();
    }

    public static int getMethodDescIndex(int minfoIndex) {
        return qbicc_method_info_table[minfoIndex].methodDescIndex.intValue();
    }

    @internal
    public static final class source_code_info extends object {
        public uint32_t minfo_index;
        public uint32_t line_number;
        public uint32_t bc_index;
        public uint32_t inlined_at_index;
    }

    public static final class source_code_info_ptr extends ptr<source_code_info> {}
    public static final class const_source_code_info_ptr extends ptr<@c_const uint32_t> {}
    public static final class source_code_info_ptr_ptr extends ptr<source_code_info_ptr> {}
    public static final class const_source_code_info_ptr_ptr extends ptr<const_source_code_info_ptr> {}
    public static final class source_code_info_ptr_const_ptr extends ptr<@c_const source_code_info_ptr> {}
    public static final class const_source_code_info_ptr_const_ptr extends ptr<@c_const const_source_code_info_ptr> {}

    @extern
    public static source_code_info[] qbicc_source_code_info_table;

    public static int getMethodInfoIndex(int scIndex) {
        return qbicc_source_code_info_table[scIndex].minfo_index.intValue();
    }

    public static int getLineNumber(int scIndex) {
        return qbicc_source_code_info_table[scIndex].line_number.intValue();
    }

    public static int getBytecodeIndex(int scIndex) {
        return qbicc_source_code_info_table[scIndex].bc_index.intValue();
    }

    @extern
    public static uint64_t[] qbicc_instruction_list;

    @extern
    public static int qbicc_instruction_list_size;

    public static uint64_t getInstructionAddress(int index) {
        return qbicc_instruction_list[index];
    }

    public static int getInstructionListSize() {
        return qbicc_instruction_list_size;
    }

    @extern
    public static uint32_t[] qbicc_source_code_index_list;

    public static uint32_t getSourceCodeInfoIndex(int index) {
        return qbicc_source_code_index_list[index];
    }
}

