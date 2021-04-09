package org.qbicc.machine.tool.clang;

import java.nio.file.Path;

import org.qbicc.machine.arch.Platform;
import io.smallrye.common.version.VersionScheme;

final class ClangToolChainImpl implements ClangToolChain {
    private final Path executablePath;
    private final Platform platform;
    private final String version;

    ClangToolChainImpl(final Path executablePath, final Platform platform, final String version) {
        this.executablePath = executablePath;
        this.platform = platform;
        this.version = version;
    }

    public String getImplementationName() {
        return "LLVM";
    }

    Path getExecutablePath() {
        return executablePath;
    }

    public ClangCCompilerInvoker newCompilerInvoker() {
        return new ClangCCompilerInvokerImpl(this);
    }

    public ClangLinkerInvoker newLinkerInvoker() {
        return new ClangLinkerInvokerImpl(this);
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
