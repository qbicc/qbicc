package cc.quarkus.qcc.tool.llvm;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.tool.Tool;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.ToolUtil;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;

/**
 * The tool provider for LLVM programs.
 */
public final class LlvmToolProvider implements ToolProvider {

    // Mac OS hides LLVM here if you install it with brew
    static final List<Path> EXTRA_PATH = List.of(Path.of("/usr/local/opt/llvm/bin"));

    public <T extends Tool> Iterable<T> findTools(final Class<T> type, final Platform platform) {
        ArrayList<T> list = new ArrayList<>();
        if (type.isAssignableFrom(LlcToolImpl.class)) {
            tryTool(type, platform, list, "llc", (path, version) -> type.cast(new LlcToolImpl(path, platform, version)));
        } else if (type.isAssignableFrom(OptToolImpl.class)) {
            tryTool(type, platform, list, "opt", (path, version) -> type.cast(new OptToolImpl(path, platform, version)));
        }
        return list;
    }

    static final Pattern VERSION_PATTERN = Pattern.compile("LLVM version (\\S+)");

    private static <T extends Tool> void tryTool(final Class<T> type, final Platform platform, final ArrayList<T> list, final String name, BiFunction<Path, String, T> factory) {
        final Path path = ToolUtil.findExecutable(name, EXTRA_PATH);
        if (path != null && Files.isExecutable(path)) {
            class Result {
                String version;
                boolean match;
            }
            Result res = new Result();
            OutputDestination dest = OutputDestination.of(r -> {
                try (BufferedReader br = new BufferedReader(r)) {
                    String line;
                    Matcher matcher;
                    while ((line = br.readLine()) != null) {
                        matcher = VERSION_PATTERN.matcher(line);
                        if (matcher.find()) {
                            res.version = matcher.group(1);
                            res.match = true;
                        }
                    }
                }
            }, StandardCharsets.UTF_8);
            ProcessBuilder pb = new ProcessBuilder(path.toString(), "--version");
            try {
                InputSource.empty().transferTo(OutputDestination.of(pb, OutputDestination.discarding(), dest));
            } catch (IOException e) {
                // ignore invalid compiler
                return;
            }
            if (res.match) {
                list.add(factory.apply(path, res.version));
            }
        }
    }

}
