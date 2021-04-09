package org.qbicc.machine.llvm;

/**
 *
 */
public interface LLBasicBlock extends LLValue {

    LLBasicBlock name(String name);

    FunctionDefinition functionDefinition();

}
