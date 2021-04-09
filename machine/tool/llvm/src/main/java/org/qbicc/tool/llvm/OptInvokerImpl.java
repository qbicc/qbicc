package org.qbicc.tool.llvm;

import java.nio.file.Path;
import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class OptInvokerImpl extends AbstractLlvmInvoker implements OptInvoker {
    private OptOptLevel optLevel = OptOptLevel.O2;

    OptInvokerImpl(final LlvmToolChainImpl tool, final Path path) {
        super(tool, path);
    }

    public LlvmToolChain getTool() {
        return super.getTool();
    }

    void addArguments(final List<String> cmd) {
        cmd.add("-" + optLevel.name());
    }

    public void setOptimizationLevel(final OptOptLevel level) {
        optLevel = Assert.checkNotNullParam("level", level);
    }

    public OptOptLevel getOptimizationLevel() {
        return optLevel;
    }
}
