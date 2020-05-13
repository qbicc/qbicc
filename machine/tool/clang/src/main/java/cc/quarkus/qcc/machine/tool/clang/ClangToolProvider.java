package cc.quarkus.qcc.machine.tool.clang;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.Tool;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.ToolUtil;

/**
 *
 */
public class ClangToolProvider implements ToolProvider {
    public <T extends Tool> Iterable<T> findTools(final Class<T> type, final Platform platform) {
        final ArrayList<T> list = new ArrayList<>();
        if (type.isAssignableFrom(ClangToolProvider.class)) {
            final Path path = ToolUtil.findExecutable("clang");
            if (path != null && Files.isExecutable(path)) {
                final ClangCompiler clang = new ClangCompiler(path);
                // todo: test it
                return List.of(type.cast(clang));
            }
        }
        return List.of();
    }
}
