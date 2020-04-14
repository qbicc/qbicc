package cc.quarkus.qcc.tool.llc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.Tool;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.ToolUtil;

/**
 * The tool provider for LLVM's {@code llc} program.
 */
public final class LLCToolProvider implements ToolProvider {

    public <T extends Tool> Iterable<T> findTools(final Class<T> type, final Platform platform) {
        if (type.isAssignableFrom(LLCTool.class)) {
            final Path path = ToolUtil.findExecutable("llc");
            if (path != null && Files.isExecutable(path)) {
                // TODO: test it
                return List.of(type.cast(new LLCTool(path)));
            } else {
                return List.of();
            }
        } else {
            return List.of();
        }
    }
}
