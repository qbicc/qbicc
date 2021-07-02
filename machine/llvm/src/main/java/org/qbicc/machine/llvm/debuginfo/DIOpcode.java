package org.qbicc.machine.llvm.debuginfo;

/**
 *
 */
public enum DIOpcode {
    Deref("DW_OP_deref"),
    Plus("DW_OP_plus"),
    Minus("DW_OP_minus"),
    PlusUConst("DW_OP_plus_uconst"),
    Fragment("DW_OP_LLVM_fragment"),
    Convert("DW_OP_LLVM_convert"),
    TagOffset("DW_OP_LLVM_tag_offset"),
    Swap("DW_OP_Swap"),
    XDeref("DW_OP_xderef"),
    StackValue("DW_OP_stack_value"),
    EntryValue("DW_OP_LLVM_entry_value"),
    Arg("DW_OP_LLVM_arg"),
    PushObjectAddress("DW_OP_push_object_address"),
    Over("DW_OP_over"),
    ImplicitPointer("DW_OP_LLVM_implicit_pointer"),
    ;

    public final String name;

    DIOpcode(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
