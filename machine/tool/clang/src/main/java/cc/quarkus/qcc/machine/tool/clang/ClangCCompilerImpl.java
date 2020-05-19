package cc.quarkus.qcc.machine.tool.clang;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;

final class ClangCCompilerImpl implements ClangCCompiler {
    private final Path executablePath;
    private final Platform platform;

    ClangCCompilerImpl(final Path executablePath, final Platform platform) {
        this.executablePath = executablePath;
        this.platform = platform;
    }

    public String getImplementationName() {
        return "LLVM";
    }

    public String getProgramName() {
        return "clang";
    }

    public Path getExecutablePath() {
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
}
