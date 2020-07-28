package cc.quarkus.qcc.tool.llvm;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;

/**
 *
 */
public class LlcToolImpl extends AbstractLlvmTool implements LlcTool {

    LlcToolImpl(final Path path, final Platform platform, final String version) {
        super(path, platform, version);
    }

    public LlcInvoker newInvoker() {
        return new LlcInvokerImpl(this);
    }
}
