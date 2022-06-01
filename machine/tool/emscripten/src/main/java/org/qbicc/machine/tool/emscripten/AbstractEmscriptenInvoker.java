package org.qbicc.machine.tool.emscripten;

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

abstract class AbstractEmscriptenInvoker implements MessagingToolInvoker {
    static final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));

    private final EmscriptenToolChainImpl tool;
    private ToolMessageHandler messageHandler = ToolMessageHandler.DISCARDING;

    AbstractEmscriptenInvoker(final EmscriptenToolChainImpl tool) {
        this.tool = tool;
    }

    public void setMessageHandler(final ToolMessageHandler messageHandler) {
        this.messageHandler = Assert.checkNotNullParam("messageHandler", messageHandler);
    }

    public ToolMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public EmscriptenToolChainImpl getTool() {
        return tool;
    }

    public Path getPath() {
        // only one executable for now
        return tool.getExecutablePath();
    }

    static final Pattern DIAG_PATTERN = Pattern.compile(
        "([^ :]+):(?:(\\d+):(?:(\\d+):)?)? (?i:(?<levelStr>error|warning|note): )?(?<msg>.*)(?: \\[-[^]]+])?");

    void collectError(final Reader reader) throws IOException {
        final ToolMessageHandler handler = getMessageHandler();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            Matcher matcher;
            while ((line = br.readLine()) != null) {
                matcher = DIAG_PATTERN.matcher(line.trim());
                ToolMessageHandler.Level level = ToolMessageHandler.Level.ERROR;
                String fileOrExecutable = "";
                String msg = line;
                int lnum = -1;
                if (matcher.matches()) {
                    fileOrExecutable = matcher.group(1);
                    try {
                        lnum = Integer.parseInt(matcher.group(2));
                    } catch (NumberFormatException e) {
                        // just keep lnum at -1
                    }
                    String levelStr = matcher.group("levelStr");
                    msg = matcher.group("msg");
                    if (levelStr != null) {
                        switch (levelStr) {
                            case "note": level = ToolMessageHandler.Level.INFO; break;
                            case "warning": level = ToolMessageHandler.Level.WARNING; break;
                            case "error": level = ToolMessageHandler.Level.ERROR; break;
                        }
                    }
                }
                // don't log potentially misleading line numbers
                handler.handleMessage(this, level, fileOrExecutable, lnum, -1, msg);
            }
        }
    }

    void addArguments(List<String> cmd) {}

    InputSource getSource() {
        return InputSource.empty();
    }

    public void invoke() throws IOException {
        OutputDestination errorHandler = OutputDestination.of(AbstractEmscriptenInvoker::collectError, this, StandardCharsets.UTF_8);
        List<String> cmd = new ArrayList<>();
        cmd.add(getTool().getExecutablePath().toString());
        addArguments(cmd);
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(cmd);
        pb.environment().put("LC_ALL", "C");
        pb.environment().put("LANG", "C");
        System.out.println(cmd);
        getSource().transferTo(OutputDestination.of(pb, errorHandler, OutputDestination.discarding(), p -> {
            int ev = p.exitValue();
            if (ev != 0) {
                throw new CompilationFailureException("Compiler terminated with exit code " + ev);
            }
        }));
    }
}
