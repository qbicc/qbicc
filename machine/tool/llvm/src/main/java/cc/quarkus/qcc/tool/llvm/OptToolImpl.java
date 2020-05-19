package cc.quarkus.qcc.tool.llvm;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;

/**
 *
 */
final class OptToolImpl extends AbstractLlvmTool implements OptTool {
    OptToolImpl(final Path path, final Platform platform) {
        super(path, platform);
    }

    public String getToolName() {
        return "LLVM Bitcode Optimizer";
    }

    public String getProgramName() {
        return "opt";
    }

    public OptInvoker newInvoker() {
        return new OptInvokerImpl(this);
    }
}
