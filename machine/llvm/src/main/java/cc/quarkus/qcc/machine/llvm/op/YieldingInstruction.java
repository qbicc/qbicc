package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.Value;

/**
 *
 */
public interface YieldingInstruction extends Instruction {
    YieldingInstruction comment(String comment);

    YieldingInstruction meta(String name, Value data);

    Value asGlobal();

    Value asGlobal(String name);

    Value asLocal();

    Value asLocal(String name);

    default Value asMetadata() {
        throw new UnsupportedOperationException();
    }

    default Value asMetadata(String name) {
        throw new UnsupportedOperationException();
    }

    default Value asAttribute() {
        throw new UnsupportedOperationException();
    }

    default Value asAttribute(String name) {
        throw new UnsupportedOperationException();
    }
}
