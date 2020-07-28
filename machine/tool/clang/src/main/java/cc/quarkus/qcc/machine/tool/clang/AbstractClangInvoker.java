package cc.quarkus.qcc.machine.tool.clang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.quarkus.qcc.machine.tool.CompilationFailureException;
import cc.quarkus.qcc.machine.tool.MessagingToolInvoker;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;
import io.smallrye.common.constraint.Assert;

abstract class AbstractClangInvoker implements MessagingToolInvoker {
    static final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));

    private final ClangCCompilerImpl tool;
    private ToolMessageHandler messageHandler = ToolMessageHandler.DISCARDING;

    AbstractClangInvoker(final ClangCCompilerImpl tool) {
        this.tool = tool;
    }

    public void setMessageHandler(final ToolMessageHandler messageHandler) {
        this.messageHandler = Assert.checkNotNullParam("messageHandler", messageHandler);
    }

    public ToolMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public ClangCCompilerImpl getTool() {
        return tool;
    }

    static final Pattern DIAG_PATTERN = Pattern.compile("([^:]+):(\\d+):(?:(\\d+):)? (?i:error|warning|note): (.*)(?: \\[-[^]]+])?");

    void collectError(final Reader reader) throws IOException {
        final ToolMessageHandler handler = getMessageHandler();
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
                    handler.handleMessage(getTool(), level, matcher.group(1), Integer.parseInt(matcher.group(2)), -1, matcher.group(5));
                }
            }
        }
    }

    void addArguments(List<String> cmd) {}

    InputSource getSource() {
        return InputSource.empty();
    }

    public void invoke() throws IOException {
        OutputDestination errorHandler = OutputDestination.of(AbstractClangInvoker::collectError, this, StandardCharsets.UTF_8);
        List<String> cmd = new ArrayList<>();
        cmd.add(getTool().getExecutablePath().toString());
        addArguments(cmd);
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(cmd);
        pb.environment().put("LC_ALL", "C");
        pb.environment().put("LANG", "C");
        getSource().transferTo(OutputDestination.of(pb, errorHandler, OutputDestination.discarding(), p -> {
            int ev = p.exitValue();
            if (ev != 0) {
                throw new CompilationFailureException("Compiler terminated with exit code " + ev);
            }
        }));
    }
}
