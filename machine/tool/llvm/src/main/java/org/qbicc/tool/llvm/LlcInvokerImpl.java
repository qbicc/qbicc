package org.qbicc.tool.llvm;

import java.nio.file.Path;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.version.VersionScheme;
import org.qbicc.machine.arch.Platform;

/**
 *
 */
final class LlcInvokerImpl extends AbstractLlvmInvoker implements LlcInvoker {
    private boolean opaquePointers = false;
    private LlcOptLevel optLevel = LlcOptLevel.O2;
    private OutputFormat outputFormat = OutputFormat.OBJ;
    private RelocationModel relocationModel = RelocationModel.Static;
    private List<String> options = List.of();

    LlcInvokerImpl(final LlvmToolChainImpl tool, final Path path) {
        super(tool, path);
    }

    public LlvmToolChain getTool() {
        return super.getTool();
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

    public void setRelocationModel(RelocationModel relocationModel) {
        this.relocationModel = Assert.checkNotNullParam("relocationModel", relocationModel);
    }

    public RelocationModel getRelocationModel() {
        return relocationModel;
    }

    public void setOpaquePointers(final boolean opaquePointers) {
        this.opaquePointers = opaquePointers;
    }

    void addArguments(final List<String> cmd) {
        LlvmToolChain tool = getTool();
        String llvmVersion = tool.getVersion();
        if (opaquePointers) {
            if (VersionScheme.BASIC.compare(llvmVersion, "15") >= 0) {
                // enabled by default on 15 or later
            } else if (VersionScheme.BASIC.compare(llvmVersion, "14") >= 0) {
                cmd.add("--opaque-pointers");
            } else {
                // 13 or later
                cmd.add("--force-opaque-pointers");
            }
        } else {
            if (VersionScheme.BASIC.compare(llvmVersion, "16") >= 0) {
                throw new IllegalArgumentException("Opaque pointers cannot be disabled on LLVM 16 or later");
            } else if (VersionScheme.BASIC.compare(llvmVersion, "15") >= 0) {
                // explicitly disable
                cmd.add("--opaque-pointers=0");
            }
        }
        Platform platform = tool.getPlatform();
        cmd.add("-mtriple=" + platform.getCpu().toString() + "-" + platform.getOs().toString() + "-" + platform.getAbi().toString());
        cmd.add("--relocation-model=" + relocationModel.value);
        cmd.add("-" + optLevel.name());
        cmd.add("--filetype=" + outputFormat.toOptionString());
        cmd.add("--dwarf-version=4");
        if (VersionScheme.BASIC.compare(getTool().getVersion(), "14") >= 0) {
            cmd.add("--strict-dwarf");
        }
        cmd.addAll(options);
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
