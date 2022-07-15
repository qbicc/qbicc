package org.qbicc.tool.llvm;

import java.util.List;

/**
 *
 */
public interface OptInvoker extends LlvmInvoker {
    LlvmToolChain getTool();

    void addOptimizationPass(OptPass level);

    void setOptions(List<String> cmd);
}
