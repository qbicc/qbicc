package cc.quarkus.qcc.tool.llvm;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.tool.Tool;

/**
 *
 */
public class OptTool extends Tool {
    private final Path path;

    OptTool(final Path path) {
        this.path = path;
    }

    public String getToolName() {
        return "LLVM Bitcode Optimizer";
    }

    public String getImplementationName() {
        return "llvm";
    }

    public String getProgramName() {
        return "opt";
    }

    public Path getExecutablePath() {
        return path;
    }

    public OptInvocationBuilder invocationBuilder() {
        return new OptInvocationBuilder(this);
    }
}
