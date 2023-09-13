package org.qbicc.machine.file.wasm.model;

import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * An instruction instance.
 */
public sealed interface Insn<I extends Op> permits AtomicMemoryAccessInsn,
                                                   BlockInsn,
                                                   ConstF32Insn,
                                                   ConstF64Insn,
                                                   ConstI32Insn,
                                                   ConstI64Insn,
                                                   ConstI128Insn,
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
                                                   MemoryAndMemoryInsn,
                                                   MultiBranchInsn,
                                                   RefTypedInsn,
                                                   SimpleInsn,
                                                   TableInsn,
                                                   TableAndFuncTypeInsn,
                                                   TableAndTableInsn,
                                                   TagInsn,
                                                   TypesInsn
{
    I op();

    <E extends Exception> void accept(final InsnSeqVisitor<E> ev, Encoder encoder) throws E;

    interface Encoder {
        int encode(BranchTarget branchTarget);

        int encode(Element element);

        int encode(Func func);

        int encode(FuncType type);

        int encode(Global global);

        int encode(Memory memory);

        int encode(Table table);

        int encode(Segment seg);

        int encode(Tag tag);
    }

    interface Resolver {
        ElementHandle resolveElement(int index);

        Func resolveFunc(int index);

        FuncType resolveFuncType(int index);

        Global resolveGlobal(int index);

        Memory resolveMemory(int index);

        Table resolveTable(int index);

        SegmentHandle resolveSegment(int index);

        Tag resolveTag(int index);
    }
}
