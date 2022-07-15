package org.qbicc.tool.llvm;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.arch.Platform;

/**
 *
 */
final class OptInvokerImpl extends AbstractLlvmInvoker implements OptInvoker {
    private List<OptPass> passes = new ArrayList<>();
    private List<String> options = List.of();

    OptInvokerImpl(final LlvmToolChainImpl tool, final Path path) {
        super(tool, path);
    }

    public LlvmToolChain getTool() {
        return super.getTool();
    }

    void addArguments(final List<String> cmd) {
        Platform platform = getTool().getPlatform();
        cmd.add("-mtriple=" + platform.getCpu().toString() + "-" + platform.getOs().toString() + "-" + platform.getAbi().toString());
        for (OptPass pass : passes) {
            cmd.add("-" + pass.name);
        }
        cmd.addAll(options);
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public void addOptimizationPass(final OptPass pass) {
        passes.add(Assert.checkNotNullParam("pass", pass));
    }
}
