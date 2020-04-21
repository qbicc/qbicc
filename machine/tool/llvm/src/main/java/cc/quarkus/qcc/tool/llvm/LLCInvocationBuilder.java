package cc.quarkus.qcc.tool.llvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.CompilationResult;
import cc.quarkus.qcc.machine.tool.InvocationBuilder;
import cc.quarkus.qcc.machine.tool.Tool;

/**
 *
 */
public class LLCInvocationBuilder extends InvocationBuilder<LLCInvocationBuilder.Param, CompilationResult> {
    private Path outputFile;
    private Platform platform = Platform.HOST_PLATFORM;

    LLCInvocationBuilder(final Tool tool) {
        super(tool);
    }

    public Platform getPlatform() {
        return platform;
    }

    public LLCInvocationBuilder setPlatform(final Platform platform) {
        this.platform = platform;
        return this;
    }

    protected Param createCollectorParam() throws IOException {
        Path outputFile = this.outputFile;
        if (outputFile == null) {
            final Path tempDirectory = Files.createTempDirectory("qcc-");
            outputFile = tempDirectory.resolve("build.s");
            outputFile.toFile().deleteOnExit();
        }
        return new Param(outputFile, platform);
    }

    protected ProcessBuilder createProcessBuilder(final Param param) {
        final ProcessBuilder pb = super.createProcessBuilder(param);
        //noinspection SpellCheckingInspection
        pb.command().addAll(List.of("-mtriple=" + param.platform, "--filetype=asm", "-mcpu=" + param.platform.getCpu().getName(), "-o=" + outputFile.toString()));
        return pb;
    }

    protected CompilationResult produceResult(final Param param, final Process process) {
        waitForProcessUninterruptibly(process);
        return new CompilationResult(param.outputFile);
    }

    static final class Param {
        final Path outputFile;
        final Platform platform;

        Param(final Path outputFile, final Platform platform) {
            this.outputFile = outputFile;
            this.platform = platform;
        }
    }
}
