package cc.quarkus.qcc.tool.llvm;

import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class OptInvokerImpl extends AbstractLlvmInvoker implements OptInvoker {
    private OptOptLevel optLevel = OptOptLevel.O2;

    OptInvokerImpl(final OptToolImpl tool) {
        super(tool);
    }

    public OptTool getTool() {
        return (OptTool) super.getTool();
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
