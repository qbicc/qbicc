package cc.quarkus.qcc.tool.llvm;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.arch.Platform;
import io.smallrye.common.version.VersionScheme;

final class LlvmToolChainImpl implements LlvmToolChain {
    private final Path llcPath;
    private final Path optPath;
    private final Platform platform;
    private final String version;

    LlvmToolChainImpl(final Path llcPath, final Path optPath, final Platform platform, final String version) {
        this.llcPath = llcPath;
        this.optPath = optPath;
        this.platform = platform;
        this.version = version;
    }

    public LlcInvoker newLlcInvoker() {
        return new LlcInvokerImpl(this, llcPath);
    }

    public OptInvoker newOptInvoker() {
        return new OptInvokerImpl(this, optPath);
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
