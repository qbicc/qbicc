package cc.quarkus.qcc.tool.llvm;

/**
 *
 */
public interface LlcTool extends LlvmTool {
    default String getToolName() {
        return "LLVM Bitcode Compiler";
    }

    default String getProgramName() {
        return "llc";
    }

    LlcInvoker newInvoker();
}
