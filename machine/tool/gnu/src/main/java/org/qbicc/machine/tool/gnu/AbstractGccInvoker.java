package org.qbicc.machine.tool.gnu;

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

import org.qbicc.machine.tool.CompilationFailureException;
import org.qbicc.machine.tool.MessagingToolInvoker;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;
import io.smallrye.common.constraint.Assert;

abstract class AbstractGccInvoker implements MessagingToolInvoker {
    static final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));

    private final GccToolChainImpl tool;
    private ToolMessageHandler messageHandler = ToolMessageHandler.DISCARDING;

    AbstractGccInvoker(final GccToolChainImpl tool) {
        this.tool = tool;
    }

    public void setMessageHandler(final ToolMessageHandler messageHandler) {
        this.messageHandler = Assert.checkNotNullParam("messageHandler", messageHandler);
    }

    public ToolMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public GccToolChainImpl getTool() {
        return tool;
    }

    public Path getPath() {
        // only one executable for now
        return tool.getExecutablePath();
    }

    static final Pattern DIAG_PATTERN = Pattern.compile("([^:]+):(?:(\\d+):(?:(\\d+):)?)? (?:((?:fatal )?error|warning|note): )?(.*)(?: \\[-[^]]+])?");

    void collectError(final Reader reader) throws IOException {
        final ToolMessageHandler handler = getMessageHandler();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            Matcher matcher;
            StringBuilder b = new StringBuilder();
            ToolMessageHandler.Level level = null;
            String file = "";
            while ((line = br.readLine()) != null) {
                matcher = DIAG_PATTERN.matcher(line.trim());
                if (matcher.matches()) {
                    if (b.length() > 0) {
                        handler.handleMessage(this, level, file, -1, -1, b.toString());
                        b.setLength(0);
                    }
                    String levelStr = matcher.group(4);
                    if (levelStr != null) {
                        switch (levelStr) {
                            case "note": level = ToolMessageHandler.Level.INFO; break;
                            case "warning": level = ToolMessageHandler.Level.WARNING; break;
                            default: level = ToolMessageHandler.Level.ERROR; break;
                        }
                    } else {
                        level = ToolMessageHandler.Level.ERROR;
                    }
                    // don't log potentially misleading line numbers
                    file = matcher.group(1);
                    b.append(matcher.group(5));
                } else {
                    b.append('\n').append(line);
                }
            }
            if (b.length() > 0) {
                handler.handleMessage(this, level, file, -1, -1, b.toString());
            }
        }
    }

    void addArguments(List<String> cmd) {}

    InputSource getSource() {
        return InputSource.empty();
    }

    public void invoke() throws IOException {
        OutputDestination errorHandler = OutputDestination.of(AbstractGccInvoker::collectError, this, StandardCharsets.UTF_8);
        List<String> cmd = new ArrayList<>();
        cmd.add(getTool().getExecutablePath().toString());
        cmd.add("-pthread");
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
