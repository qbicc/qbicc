package cc.quarkus.qcc.machine.llvm;

/**
 *
 */
public interface LLBasicBlock extends LLValue {

    LLBasicBlock name(String name);

    FunctionDefinition functionDefinition();

}
