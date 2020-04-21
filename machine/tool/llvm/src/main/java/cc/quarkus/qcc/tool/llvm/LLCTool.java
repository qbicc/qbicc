package cc.quarkus.qcc.tool.llvm;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.tool.Tool;

/**
 *
 */
public class LLCTool extends Tool {
    private final Path path;

    LLCTool(final Path path) {
        this.path = path;
    }

    public String getToolName() {
        return "LLVM Bitcode Compiler";
    }

    public String getImplementationName() {
        return "llc";
    }

    public String getProgramName() {
        return "llc";
    }

    public Path getExecutablePath() {
        return path;
    }

    public LLCInvocationBuilder invocationBuilder() {
        return new LLCInvocationBuilder(this);
    }
}
