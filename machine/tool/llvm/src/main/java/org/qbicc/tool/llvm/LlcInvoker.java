package org.qbicc.tool.llvm;

/**
 *
 */
public interface LlcInvoker extends LlvmInvoker {
    LlvmToolChain getTool();

    void setOptimizationLevel(LlcOptLevel level);

    LlcOptLevel getOptimizationLevel();

    void setOutputFormat(OutputFormat outputFormat);

    OutputFormat getOutputFormat();

    void setRelocationModel(RelocationModel relocationModel);

    RelocationModel getRelocationModel();
}
