package org.qbicc.tool.llvm;

import java.nio.file.Path;

import org.qbicc.machine.arch.Platform;
import io.smallrye.common.version.VersionScheme;

final class LlvmToolChainImpl implements LlvmToolChain {
    private final Path llcPath;
    private final Path optPath;
    private final Path objCopyPath;
    private final Platform platform;
    private final String version;

    LlvmToolChainImpl(final Path llcPath, final Path optPath, Path objCopyPath, final Platform platform, final String version) {
        this.llcPath = llcPath;
        this.optPath = optPath;
        this.objCopyPath = objCopyPath;
        this.platform = platform;
        this.version = version;
    }

    public LlcInvoker newLlcInvoker() {
        return new LlcInvokerImpl(this, llcPath);
    }

    public OptInvoker newOptInvoker() {
        return new OptInvokerImpl(this, optPath);
    }

    public LlvmObjCopyInvoker newLlvmObjCopyInvoker() {
        return new LlvmObjCopyInvokerImpl(this, objCopyPath);
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
