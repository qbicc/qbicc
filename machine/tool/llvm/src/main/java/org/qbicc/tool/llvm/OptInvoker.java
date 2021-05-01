package org.qbicc.tool.llvm;

/**
 *
 */
public interface OptInvoker extends LlvmInvoker {
    LlvmToolChain getTool();

    void addOptimizationPass(OptPass level);
}
