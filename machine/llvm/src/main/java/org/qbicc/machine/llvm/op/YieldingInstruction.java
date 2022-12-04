package org.qbicc.machine.llvm.op;

import org.qbicc.machine.llvm.LLValue;

/**
 *
 */
public interface YieldingInstruction extends Instruction {
    YieldingInstruction comment(String comment);

    YieldingInstruction meta(String name, LLValue data);

    LLValue asGlobal();

    LLValue asGlobal(String name);

    LLValue asLocal();

    LLValue asLocal(String name);

    LLValue setLValue(LLValue value);

    default LLValue asMetadata() {
        throw new UnsupportedOperationException();
    }

    default LLValue asMetadata(String name) {
        throw new UnsupportedOperationException();
    }

    default LLValue asAttribute() {
        throw new UnsupportedOperationException();
    }

    default LLValue asAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    LLValue getLValue();
}
