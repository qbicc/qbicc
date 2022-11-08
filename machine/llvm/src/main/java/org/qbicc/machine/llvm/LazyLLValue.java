package org.qbicc.machine.llvm;

/**
 * An LLVM value which is lazily resolved.
 */
public interface LazyLLValue extends LLValue {
    void resolveTo(LLValue value) throws IllegalStateException;
}
