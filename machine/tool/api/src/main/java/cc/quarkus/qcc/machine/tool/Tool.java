package cc.quarkus.qcc.machine.tool;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;

/**
 * An invokable tool which may support specific target environments.
 */
public abstract class Tool {
    protected Tool() {
    }

    public abstract String getToolName();

    public abstract String getImplementationName();

    public abstract String getProgramName();

    public abstract Path getExecutablePath();

    public boolean supportsPlatform(Platform platform) {
        return false;
    }
}
