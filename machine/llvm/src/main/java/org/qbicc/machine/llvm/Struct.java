package org.qbicc.machine.llvm;

/**
 *
 */
public interface Struct extends LLValue {
    Struct item(LLValue type, LLValue value);
}
