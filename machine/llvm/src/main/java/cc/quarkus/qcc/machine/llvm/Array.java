package cc.quarkus.qcc.machine.llvm;

/**
 *
 */
public interface Array extends LLValue {
    Array item(LLValue value);
}
