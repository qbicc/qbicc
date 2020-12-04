package cc.quarkus.qcc.tool.llvm;

/**
 *
 */
public interface OptInvoker extends LlvmInvoker {
    LlvmToolChain getTool();

    void setOptimizationLevel(OptOptLevel level);

    OptOptLevel getOptimizationLevel();
}
