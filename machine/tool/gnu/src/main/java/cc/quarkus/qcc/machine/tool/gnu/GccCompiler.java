package cc.quarkus.qcc.machine.tool.gnu;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.CCompiler;

/**
 *
 */
public class GccCompiler extends CCompiler {
    private final Path executablePath;

    public GccCompiler(final Path executablePath) {
        this.executablePath = executablePath;
    }

    public String getImplementationName() {
        return "gnu";
    }

    public String getProgramName() {
        return "gcc";
    }

    public Path getExecutablePath() {
        return executablePath;
    }

    public boolean supportsPlatform(final Platform platform) {
        return super.supportsPlatform(platform);
    }

    public GccInvocationBuilder invocationBuilder() {
        return new GccInvocationBuilder(this);
    }
}
