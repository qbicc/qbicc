package cc.quarkus.qcc.machine.tool;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;

/**
 * An invokable tool which may support specific target environments.
 */
public interface Tool {
    String getToolName();

    String getImplementationName();

    String getProgramName();

    Path getExecutablePath();

    /**
     * Get the platform that this tool instance is configured for.
     *
     * @return the platform (not {@code null})
     */
    Platform getPlatform();
}
