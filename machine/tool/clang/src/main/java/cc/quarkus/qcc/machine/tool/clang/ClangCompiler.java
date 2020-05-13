package cc.quarkus.qcc.machine.tool.clang;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.CCompiler;

/**
 *
 */
public class ClangCompiler extends CCompiler {
    private final Path executablePath;

    public ClangCompiler(final Path executablePath) {
        this.executablePath = executablePath;
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

    public boolean supportsPlatform(final Platform platform) {
        return super.supportsPlatform(platform);
    }

    public ClangInvocationBuilder invocationBuilder() {
        return new ClangInvocationBuilder(this);
    }
}
