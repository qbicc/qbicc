package cc.quarkus.qcc.tool.llvm;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;
import io.smallrye.common.version.VersionScheme;

/**
 *
 */
abstract class AbstractLlvmTool implements LlvmTool {
    private final Path path;
    private final Platform platform;
    private final String version;

    AbstractLlvmTool(final Path path, final Platform platform, final String version) {
        this.path = path;
        this.platform = platform;
        this.version = version;
    }

    public Path getExecutablePath() {
        return path;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getVersion() {
        return version;
    }

    public int compareVersionTo(final String version) {
        return VersionScheme.BASIC.compare(this.version, version);
    }
}
