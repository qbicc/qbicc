package cc.quarkus.qcc.machine.tool.gnu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import cc.quarkus.qcc.machine.tool.Tool;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.ToolUtil;

/**
 *
 */
public class GccToolProvider implements ToolProvider {
    public <T extends Tool> Iterable<T> findTools(final Class<T> type) {
        if (type.isAssignableFrom(GccCompiler.class)) {
            final Path path = ToolUtil.findExecutable("gcc");
            if (path != null && Files.isExecutable(path)) {
                return List.of(type.cast(new GccCompiler(path)));
            }
        }
        return List.of();
    }

}
