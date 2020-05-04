package cc.quarkus.qcc.machine.tool.gnu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.quarkus.qcc.machine.tool.CompilationFailureException;
import cc.quarkus.qcc.machine.tool.CompilerInvokerBuilder;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;

public final class GccInvocationBuilder extends CompilerInvokerBuilder {
    private static final long pid = ProcessHandle.current().pid();
    private static final String tmpDir = System.getProperty("java.io.tmpdir");
    private static final AtomicInteger cnt = new AtomicInteger();

    GccInvocationBuilder(final GccCompiler gcc) {
        super(gcc);
    }

    public OutputDestination build() {
        OutputDestination errorHandler = OutputDestination.of(GccInvocationBuilder::collectError, getMessageHandler(), StandardCharsets.UTF_8);
        List<String> cmd = new ArrayList<>();
        cmd.add(getTool().getExecutablePath().toString());
        Collections.addAll(cmd, "-std=gnu11", "-f" + "input-charset=UTF-8", "-pipe");
        for (Path includePath : getIncludePaths()) {
            cmd.add("-I" + includePath.toString());
        }
        Collections.addAll(cmd, "-c", "-x", "c", "-o", getOutputPath().toString(), "-");
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(cmd);
        pb.environment().put("LC_ALL", "C");
        pb.environment().put("LANG", "C");
        return OutputDestination.of(pb, errorHandler, OutputDestination.discarding(), p -> {
            int ev = p.exitValue();
            if (ev != 0) {
                throw new CompilationFailureException("Compiler terminated with exit code " + ev);
            }
        });
    }

    static final Pattern DIAG_PATTERN = Pattern.compile("([^:]+):(\\d+):(?:(\\d+):)? (error|warning|note): (.*)(?: \\[-[^]]+])?");

    static void collectError(final ToolMessageHandler handler, final Reader reader) throws IOException {
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            Matcher matcher;
            while ((line = br.readLine()) != null) {
                matcher = DIAG_PATTERN.matcher(line.trim());
                if (matcher.matches()) {
                    String levelStr = matcher.group(4);
                    ToolMessageHandler.Level level;
                    switch (levelStr) {
                        case "note": level = ToolMessageHandler.Level.INFO; break;
                        case "warning": level = ToolMessageHandler.Level.WARNING; break;
                        default: level = ToolMessageHandler.Level.ERROR; break;
                    }
                    // don't log potentially misleading line numbers
                    handler.handleMessage(level, matcher.group(1), Integer.parseInt(matcher.group(2)), -1, matcher.group(5));
                }
            }
        }
    }
}
