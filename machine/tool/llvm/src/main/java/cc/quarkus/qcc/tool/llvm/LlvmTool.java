package cc.quarkus.qcc.tool.llvm;

import cc.quarkus.qcc.machine.tool.Tool;

/**
 *
 */
public interface LlvmTool extends Tool {
    default String getImplementationName() {
        return "llvm";
    }

    LlvmInvoker newInvoker();
}
