package org.qbicc.plugin.llvm;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.arch.Platform;

/**
 * Configuration of the LLVM plugin used by classes within the plugin.
 */
public final class LLVMConfiguration {
    private final Platform platform;
    private final int majorVersion;
    private final boolean pie;
    private final boolean statepointEnabled;
    private final boolean emitIr;
    private final boolean emitAssembly;
    private final boolean compileOutput;
    private final List<String> llcOptions;
    private final ReferenceStrategy referenceStrategy;

    LLVMConfiguration(Builder builder) {
        platform = Assert.checkNotNullParam("builder.platform", builder.platform);
        majorVersion = builder.majorVersion;
        pie = builder.pie;
        statepointEnabled = builder.statepointEnabled;
        emitIr = builder.emitIr;
        emitAssembly = builder.emitAssembly;
        List<String> builderLlcOptions = builder.llcOptions;
        if (builderLlcOptions == null) {
            llcOptions = List.of();
        } else {
            llcOptions = List.copyOf(builder.llcOptions);
        }
        compileOutput = builder.compileOutput;
        referenceStrategy = builder.referenceStrategy;
    }

    public Platform getPlatform() {
        return platform;
    }

    public boolean isWasm() {
        return getPlatform().isWasm();
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public boolean isPie() {
        return pie;
    }

    public boolean isStatepointEnabled() {
        return statepointEnabled;
    }

    public boolean isEmitIr() {
        return emitIr;
    }

    public boolean isEmitAssembly() {
        return emitAssembly;
    }

    public boolean isCompileOutput() {
        return compileOutput;
    }

    public List<String> getLlcOptions() {
        return llcOptions;
    }

    public ReferenceStrategy getReferenceStrategy() {
        return referenceStrategy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Platform platform;
        private int majorVersion = 15;
        private boolean pie = true;
        private boolean statepointEnabled = true;
        private boolean emitIr;
        private boolean emitAssembly;
        private boolean compileOutput;
        private List<String> llcOptions;
        private ReferenceStrategy referenceStrategy = ReferenceStrategy.POINTER_AS1;

        Builder() {}

        public Platform getPlatform() {
            return platform;
        }

        public Builder setPlatform(Platform platform) {
            Assert.checkNotNullParam("platform", platform);
            this.platform = platform;
            return this;
        }

        public int getMajorVersion() {
            return majorVersion;
        }

        public Builder setMajorVersion(int majorVersion) {
            this.majorVersion = majorVersion;
            return this;
        }

        public boolean isPie() {
            return pie;
        }

        public Builder setPie(boolean pie) {
            this.pie = pie;
            return this;
        }

        public boolean isStatepointEnabled() {
            return statepointEnabled;
        }

        public Builder setStatepointEnabled(boolean statepointEnabled) {
            this.statepointEnabled = statepointEnabled;
            return this;
        }

        public boolean isEmitIr() {
            return emitIr;
        }

        public Builder setEmitIr(boolean emitIr) {
            this.emitIr = emitIr;
            return this;
        }

        public boolean isEmitAssembly() {
            return emitAssembly;
        }

        public Builder setEmitAssembly(boolean emitAssembly) {
            this.emitAssembly = emitAssembly;
            return this;
        }

        public boolean isCompileOutput() {
            return compileOutput;
        }

        public Builder setCompileOutput(boolean compileOutput) {
            this.compileOutput = compileOutput;
            return this;
        }

        public Builder addLlcOption(String option) {
            Assert.checkNotNullParam("option", option);
            if (llcOptions == null) {
                llcOptions = new ArrayList<>();
            }
            llcOptions.add(option);
            return this;
        }

        public Builder addLlcOptions(List<String> options) {
            Assert.checkNotNullParam("options", options);
            if (llcOptions == null) {
                llcOptions = new ArrayList<>();
            }
            llcOptions.addAll(options);
            return this;
        }

        public ReferenceStrategy getReferenceStrategy() {
            return referenceStrategy;
        }

        public Builder setReferenceStrategy(ReferenceStrategy referenceStrategy) {
            this.referenceStrategy = referenceStrategy;
            return this;
        }

        public LLVMConfiguration build() {
            return new LLVMConfiguration(this);
        }
    }
}
