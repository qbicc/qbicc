package cc.quarkus.qcc.tool.llvm;

/**
 *
 */
public interface OptInvoker extends LlvmInvoker {
    OptTool getTool();

    void setOptimizationLevel(OptOptLevel level);

    OptOptLevel getOptimizationLevel();
}
