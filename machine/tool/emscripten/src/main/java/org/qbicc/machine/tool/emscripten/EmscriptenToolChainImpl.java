package org.qbicc.machine.tool.emscripten;

import java.nio.file.Path;

import org.qbicc.machine.arch.Platform;
import io.smallrye.common.version.VersionScheme;

final class EmscriptenToolChainImpl implements EmscriptenToolChain {
    private final Path executablePath;
    private final Platform platform;
    private final String version;

    EmscriptenToolChainImpl(final Path executablePath, final Platform platform, final String version) {
        this.executablePath = executablePath;
        this.platform = platform;
        this.version = version;
    }

    public String getImplementationName() {
        return "Emscripten (LLVM)";
    }

    Path getExecutablePath() {
        return executablePath;
    }

    public EmscriptenCCompilerInvoker newCompilerInvoker() {
        return new EmscriptenCCompilerInvokerImpl(this);
    }

    public EmscriptenLinkerInvoker newLinkerInvoker() {
        return new EmscriptenLinkerInvokerImpl(this);
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
