package org.qbicc.machine.tool;

import org.qbicc.machine.arch.Platform;

/**
 * An invokable tool which may support specific target environments.
 */
public interface Tool {
    String getToolName();

    String getImplementationName();

    /**
     * Get the platform that this tool instance is configured for.
     *
     * @return the platform (not {@code null})
     */
    Platform getPlatform();

    /**
     * Get the tool version as a string.
     *
     * @return the tool version
     */
    String getVersion();

    /**
     * Compare the version of this tool to the given version, using the tool's version scheme.
     *
     * @param version the version to compare against (must not be {@code null})
     * @return {@code -1}, {@code 0}, or {@code 1} if the tool version is older than, the same as, or newer than the
     *         given version.
     */
    int compareVersionTo(String version);
}
