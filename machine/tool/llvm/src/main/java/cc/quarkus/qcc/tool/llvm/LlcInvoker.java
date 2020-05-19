package cc.quarkus.qcc.tool.llvm;

/**
 *
 */
public interface LlcInvoker extends LlvmInvoker {
    LlcTool getTool();

    void setOptimizationLevel(LlcOptLevel level);

    LlcOptLevel getOptimizationLevel();

    void setOutputFormat(OutputFormat outputFormat);

    OutputFormat getOutputFormat();
}
