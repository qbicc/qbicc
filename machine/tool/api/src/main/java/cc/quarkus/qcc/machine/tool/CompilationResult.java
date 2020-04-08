package cc.quarkus.qcc.machine.tool;

import java.nio.file.Path;

/**
 *
 */
public class CompilationResult {
    private final Path objectFilePath;

    public CompilationResult(final Path objectFilePath) {
        this.objectFilePath = objectFilePath;
    }

    public Path getObjectFilePath() {
        return objectFilePath;
    }
}
