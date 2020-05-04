package cc.quarkus.qcc.machine.tool.gnu;

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
public class GccToolProvider implements ToolProvider {
    public <T extends Tool> Iterable<T> findTools(final Class<T> type, final Platform platform) {
        final ArrayList<T> list = new ArrayList<>();
        if (type.isAssignableFrom(GccCompiler.class)) {
            final String cpuSimpleName = platform.getCpu().getSimpleName();
            final String osName = platform.getOs().getName();
            final String abiName = platform.getAbi().getName();
            // prefer host CC
            for (String name : List.of("gcc", cpuSimpleName + "-" + osName + "-" + abiName + "-gcc")) {
                final Path path = ToolUtil.findExecutable(name);
                if (path != null && Files.isExecutable(path)) {
                    final GccCompiler gcc = new GccCompiler(path);
                    // todo: test it
                    list.add(type.cast(gcc));
                }
            }
            return list;
        } else {
            return List.of();
        }
    }

}
