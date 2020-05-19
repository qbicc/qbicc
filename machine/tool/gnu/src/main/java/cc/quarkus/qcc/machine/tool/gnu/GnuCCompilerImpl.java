package cc.quarkus.qcc.machine.tool.gnu;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;

/**
 *
 */
final class GnuCCompilerImpl implements GnuCCompiler {
    private final Path executablePath;
    private final Platform platform;

    GnuCCompilerImpl(final Path executablePath, final Platform platform) {
        this.executablePath = executablePath;
        this.platform = platform;
    }

    public Path getExecutablePath() {
        return executablePath;
    }

    public Platform getPlatform() {
        return platform;
    }

    public GnuCCompilerInvoker newCompilerInvoker() {
        return new GnuCCompilerInvokerImpl(this);
    }

    public GnuLinkerInvoker newLinkerInvoker() {
        return new GnuLinkerInvokerImpl(this);
    }
}
