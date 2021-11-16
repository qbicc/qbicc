package org.qbicc.tool.llvm;

import java.util.List;

/**
 *
 */
public interface OptInvoker extends LlvmInvoker {
    LlvmToolChain getTool();

    void setOpaquePointers(final boolean opaquePointers);

    void addOptimizationPass(OptPass level);

    void setOptions(List<String> cmd);
}
