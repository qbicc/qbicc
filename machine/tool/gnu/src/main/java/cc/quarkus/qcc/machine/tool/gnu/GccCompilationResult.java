package cc.quarkus.qcc.machine.tool.gnu;

import java.nio.file.Path;

import cc.quarkus.qcc.machine.tool.CompilationResult;

/**
 *
 */
public class GccCompilationResult extends CompilationResult {
    GccCompilationResult(final Path objectPath) {
        super(objectPath);
    }
}
