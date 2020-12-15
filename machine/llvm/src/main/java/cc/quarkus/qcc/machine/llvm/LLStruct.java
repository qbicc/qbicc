package cc.quarkus.qcc.machine.llvm;

/**
 * A struct type.
 */
public interface LLStruct extends LLValue {
    /**
     * Add a member to this struct type.
     *
     * @param type the type to add
     * @return this struct type
     */
    LLStruct member(LLValue type);
}
