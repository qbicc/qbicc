package cc.quarkus.qcc.machine.tool;

import java.nio.file.Path;

/**
 *
 */
public abstract class Tool {
    protected Tool() {
    }

    public abstract String getToolName();

    public abstract String getImplementationName();

    public abstract String getProgramName();

    public abstract Path getExecutablePath();
}
