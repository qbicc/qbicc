package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction instance.
 */
public sealed interface Insn<I extends Op> permits AtomicMemoryAccessInsn,
                                                   BlockInsn,
                                                   ConstF32Insn,
                                                   ConstF64Insn,
                                                   ConstI32Insn,
                                                   ConstI64Insn,
                                                   ConstV128Insn,
                                                   DataInsn,
                                                   ElementInsn,
                                                   ElementAndTableInsn,
                                                   ExceptionInsn,
                                                   FuncInsn,
                                                   GlobalInsn,
                                                   BranchInsn,
                                                   LaneInsn,
                                                   LocalInsn,
                                                   MemoryInsn,
                                                   MemoryAccessInsn,
                                                   MemoryAccessLaneInsn,
                                                   MemoryAndDataInsn,
                                                   MemoryToMemoryInsn,
                                                   MultiBranchInsn,
                                                   RefTypedInsn,
                                                   SimpleInsn,
                                                   TableInsn,
                                                   TableAndFuncTypeInsn,
                                                   TableToTableInsn,
                                                   TagInsn,
                                                   TypesInsn
{
    I op();

    void writeTo(WasmOutputStream wos, Encoder encoder) throws IOException;

}
