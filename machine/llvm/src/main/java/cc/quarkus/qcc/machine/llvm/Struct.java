package cc.quarkus.qcc.machine.llvm;

/**
 *
 */
public interface Struct extends LLValue {
    Struct item(LLValue type, LLValue value);
}
