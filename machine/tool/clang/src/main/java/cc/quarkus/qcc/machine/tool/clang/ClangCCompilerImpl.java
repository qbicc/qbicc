package cc.quarkus.qcc.machine.tool.clang;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;
import io.smallrye.common.version.VersionScheme;

final class ClangCCompilerImpl implements ClangCCompiler {
    private final Path executablePath;
    private final Platform platform;
    private final String version;

    ClangCCompilerImpl(final Path executablePath, final Platform platform, final String version) {
        this.executablePath = executablePath;
        this.platform = platform;
        this.version = version;
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

    public String getVersion() {
        return version;
    }

    public int compareVersionTo(final String version) {
        return VersionScheme.BASIC.compare(this.version, version);
    }
}
