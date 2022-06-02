package org.qbicc.machine.tool.emscripten;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.tool.Tool;
import org.qbicc.machine.tool.ToolProvider;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;

/**
 *
 */
public class EmscriptenToolProvider implements ToolProvider {
    public <T extends Tool> Iterable<T> findTools(final Class<T> type, final Platform platform, final Path path) {
        final ArrayList<T> list = new ArrayList<>();
        if (type.isAssignableFrom(EmscriptenToolChainImpl.class)) {
            tryOne(type, platform, list, path);
        }
        return list;
    }

    static final Pattern VERSION_PATTERN = Pattern.compile("^emcc \\(Emscripten .+\\) (.+)$");

    private <T extends Tool> void tryOne(final Class<T> type, final Platform platform, final ArrayList<T> list, final Path path) {
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
            ProcessBuilder pb = new ProcessBuilder(path.toString(), "-v");
            try {
                InputSource.empty().transferTo(OutputDestination.of(pb, dest, OutputDestination.discarding()));
            } catch (IOException e) {
                // ignore invalid compiler
                return;
            }
            if (res.match) {
                final EmscriptenToolChainImpl cc = new EmscriptenToolChainImpl(path, platform, res.version);
                list.add(type.cast(cc));
            }
        }
    }
}
