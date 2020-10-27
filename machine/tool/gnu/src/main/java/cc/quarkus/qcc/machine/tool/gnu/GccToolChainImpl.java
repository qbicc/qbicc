package cc.quarkus.qcc.machine.tool.gnu;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;
import io.smallrye.common.version.VersionScheme;

/**
 *
 */
final class GccToolChainImpl implements GccToolChain {
    private final Path executablePath;
    private final Platform platform;
    private final String version;
    private final boolean m32;

    GccToolChainImpl(final Path executablePath, final Platform platform, final String version, final boolean m32) {
        this.executablePath = executablePath;
        this.platform = platform;
        this.version = version;
        this.m32 = m32;
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

    public String getVersion() {
        return version;
    }

    public int compareVersionTo(final String version) {
        return VersionScheme.BASIC.compare(this.version, version);
    }

    boolean isM32() {
        return m32;
    }
}
