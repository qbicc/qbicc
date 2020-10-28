package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLValue;

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
}
