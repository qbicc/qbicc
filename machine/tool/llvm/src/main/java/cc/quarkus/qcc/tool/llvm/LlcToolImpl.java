package cc.quarkus.qcc.tool.llvm;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;

/**
 *
 */
public class LlcToolImpl extends AbstractLlvmTool implements LlcTool {

    LlcToolImpl(final Path path, final Platform platform) {
        super(path, platform);
    }

    public LlcInvoker newInvoker() {
        return new LlcInvokerImpl(this);
    }
}
