package org.qbicc.tool.llvm;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.version.VersionScheme;
import org.qbicc.machine.arch.Platform;

/**
 *
 */
final class OptInvokerImpl extends AbstractLlvmInvoker implements OptInvoker {
    private boolean opaquePointers;
    private List<OptPass> passes = new ArrayList<>();
    private List<String> options = List.of();

    OptInvokerImpl(final LlvmToolChainImpl tool, final Path path) {
        super(tool, path);
    }

    public LlvmToolChain getTool() {
        return super.getTool();
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
