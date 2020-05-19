package cc.quarkus.qcc.tool.llvm;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;

/**
 *
 */
abstract class AbstractLlvmTool implements LlvmTool {
    private final Path path;
    private final Platform platform;

    AbstractLlvmTool(final Path path, final Platform platform) {
        this.path = path;
        this.platform = platform;
    }

    public Path getExecutablePath() {
        return path;
    }

    public Platform getPlatform() {
        return platform;
    }
}
