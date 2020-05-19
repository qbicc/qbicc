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
 * The provider for GNU tools.
 */
public class GnuToolProvider implements ToolProvider {
    public GnuToolProvider() {
    }

    public <T extends Tool> Iterable<T> findTools(final Class<T> type, final Platform platform) {
        final ArrayList<T> list = new ArrayList<>();
        if (type.isAssignableFrom(GnuCCompilerImpl.class)) {
            final String cpuSimpleName = platform.getCpu().getSimpleName();
            final String osName = platform.getOs().getName();
            final String abiName = platform.getAbi().getName();
            // prefer host CC
            for (String name : List.of("gcc", cpuSimpleName + "-" + osName + "-" + abiName + "-gcc")) {
                tryGcc(type, platform, list, name);
            }
            return list;
        } else {
            return List.of();
        }
    }

    private <T extends Tool> void tryGcc(final Class<T> type, final Platform platform, final ArrayList<T> list, final String name) {
        final Path path = ToolUtil.findExecutable(name);
        if (path != null && Files.isExecutable(path)) {
            final GnuCCompilerImpl gcc = new GnuCCompilerImpl(path, platform);
            // todo: test it
            list.add(type.cast(gcc));
        }
    }
}
