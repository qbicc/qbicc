package cc.quarkus.qcc.tool.llvm;

import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class LlcInvokerImpl extends AbstractLlvmInvoker implements LlcInvoker {
    private LlcOptLevel optLevel = LlcOptLevel.O2;
    private OutputFormat outputFormat = OutputFormat.OBJ;

    LlcInvokerImpl(final LlcToolImpl tool) {
        super(tool);
    }

    public LlcTool getTool() {
        return (LlcTool) super.getTool();
    }

    public void setOptimizationLevel(final LlcOptLevel level) {
        optLevel = Assert.checkNotNullParam("level", level);
    }

    public LlcOptLevel getOptimizationLevel() {
        return optLevel;
    }

    public void setOutputFormat(final OutputFormat outputFormat) {
        this.outputFormat = Assert.checkNotNullParam("outputFormat", outputFormat);
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    void addArguments(final List<String> cmd) {
        cmd.add("-" + optLevel.name());
        cmd.add("--filetype=" + outputFormat.toOptionString());
    }
}
