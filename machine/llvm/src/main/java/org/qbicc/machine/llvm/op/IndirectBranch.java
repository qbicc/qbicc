package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLBasicBlock;
import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface IndirectBranch extends Branch {
    IndirectBranch meta(String name, LLValue data);

    IndirectBranch comment(String comment);

    IndirectBranch possibleTarget(LLBasicBlock target);
}
