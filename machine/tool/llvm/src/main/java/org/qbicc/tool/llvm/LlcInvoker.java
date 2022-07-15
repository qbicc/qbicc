package org.qbicc.tool.llvm;

import java.util.List;

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

    void setOptions(final List<String> cmd);
}
