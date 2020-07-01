package cc.quarkus.qcc.tool.llvm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.Tool;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.ToolUtil;

/**
 * The tool provider for LLVM programs.
 */
public final class LlvmToolProvider implements ToolProvider {

    // Mac OS hides LLVM here if you install it with brew
    static final List<Path> EXTRA_PATH = List.of(Path.of("/usr/local/opt/llvm/bin"));

    public <T extends Tool> Iterable<T> findTools(final Class<T> type, final Platform platform) {
        if (type.isAssignableFrom(LlcToolImpl.class)) {
            final Path path = ToolUtil.findExecutable("llc", EXTRA_PATH);
            if (path != null && Files.isExecutable(path)) {
                // TODO: test it
                return List.of(type.cast(new LlcToolImpl(path, platform)));
            } else {
                return List.of();
            }
        } else if (type.isAssignableFrom(OptToolImpl.class)) {
            final Path path = ToolUtil.findExecutable("opt", EXTRA_PATH);
            if (path != null && Files.isExecutable(path)) {
                // TODO: test it
                return List.of(type.cast(new OptToolImpl(path, platform)));
            } else {
                return List.of();
            }
        } else {
            return List.of();
        }
    }
}
