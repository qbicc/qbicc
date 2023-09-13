package org.qbicc.machine.file.wasm.stream;

import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.ValType;

/**
 *
 */
public class InsnSeqVisitor<E extends Exception> extends Visitor<E> {
    public void visit(Op.AtomicMemoryAccess insn, int memory, int offset) throws E {
    }

    public final void visit(Op.AtomicMemoryAccess insn, int offset) throws E {
        visit(insn, 0, offset);
    }

    public void visit(Op.Block insn) throws E {
    }

    public void visit(Op.Block insn, int typeIdx) throws E {
    }

    public void visit(Op.Block insn, ValType valType) throws E {
    }

    public void visit(Op.ConstF32 insn, float val) throws E {
    }

    public void visit(Op.ConstF64 insn, double val) throws E {
    }

    public void visit(Op.ConstI32 insn, int val) throws E {
    }

    public void visit(Op.ConstI64 insn, long val) throws E {
    }

    public void visit(Op.ConstI128 insn, long lowVal, long highVal) throws E {
    }

    public void visit(Op.Data insn, int dataIdx) throws E {
    }

    public void visit(Op.Element insn, int elemIdx) throws E {
    }

    public void visit(Op.ElementAndTable insn, int elemIdx, int tableIdx) throws E {
    }

    public void visit(Op.Exception insn, int blockIdx) throws E {
    }

    public void visit(Op.Func insn, int funcIdx) throws E {
    }

    public void visit(Op.Global insn, int globalIdx) throws E {
    }

    public void visit(Op.Local insn, int index) throws E {
    }

    public void visit(Op.Branch insn, int index) throws E {
    }

    public void visit(Op.Lane insn, int laneIdx) throws E {
    }

    public void visit(Op.Memory insn, int memory) throws E {
    }

    public void visit(Op.MemoryAccess insn, int memory, int align, int offset) throws E {
    }

    public final void visit(Op.MemoryAccess insn, int align, int offset) throws E {
        visit(insn, 0, align, offset);
    }

    public void visit(Op.MemoryAccessLane insn, int memory, int align, int offset, int laneIdx) throws E {
    }

    public void visit(Op.MemoryAndData insn, int dataIdx, int memIdx) throws E {
    }

    public void visit(Op.MemoryAndMemory insn, int memIdx1, int memIdx2) throws E {
    }

    public void visit(Op.Simple insn) throws E {
    }

    public void visit(Op.MultiBranch insn, int defIndex, int... targetIndexes) throws E {
    }

    public void visit(Op.Types insn, ValType... types) throws E {
    }

    public void visit(Op.Table insn, int index) throws E {
    }

    public void visit(Op.TableAndTable insn, int index1, int index2) throws E {
    }

    public void visit(Op.TableAndFuncType insn, int tableIdx, int typeIdx) throws E {
    }

    public void visit(Op.Tag insn, int index) throws E {
    }

    public void visit(Op.RefTyped insn, RefType type) throws E {
    }

    public final void visit(Op.ConstI128 insn, long lowVal) throws E {
        visit(insn, lowVal, 0L);
    }
}
