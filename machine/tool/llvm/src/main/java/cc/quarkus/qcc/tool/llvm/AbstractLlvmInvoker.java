package cc.quarkus.qcc.tool.llvm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.quarkus.qcc.machine.tool.CompilationFailureException;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
abstract class AbstractLlvmInvoker implements LlvmInvoker {
    private static final String LEVEL_PATTERN = "(?i:error|warning|note)";
    protected final LlvmToolChainImpl tool;
    private final Path execPath;
    private InputSource source = InputSource.empty();
    private ToolMessageHandler messageHandler = ToolMessageHandler.DISCARDING;
    private OutputDestination destination = OutputDestination.discarding();

    AbstractLlvmInvoker(final LlvmToolChainImpl tool, final Path execPath) {
        this.tool = tool;
        this.execPath = execPath;
    }

    public LlvmToolChain getTool() {
        return tool;
    }

    public Path getPath() {
        return execPath;
    }

    public void setSource(final InputSource source) {
        this.source = Assert.checkNotNullParam("source", source);
    }

    public InputSource getSource() {
        return source;
    }

    public void setMessageHandler(final ToolMessageHandler messageHandler) {
        this.messageHandler = Assert.checkNotNullParam("messageHandler", messageHandler);
    }

    public ToolMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setDestination(final OutputDestination destination) {
        this.destination = Assert.checkNotNullParam("destination", destination);
    }

    public OutputDestination getDestination() {
        return destination;
    }

    public OutputDestination invokerAsDestination() {
        OutputDestination errorHandler = OutputDestination.of(AbstractLlvmInvoker::collectError, this, StandardCharsets.UTF_8);
        List<String> cmd = new ArrayList<>();
        cmd.add(execPath.toString());
        addArguments(cmd);
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(cmd);
        pb.environment().put("LC_ALL", "C");
        pb.environment().put("LANG", "C");
        return OutputDestination.of(pb, errorHandler, destination, p -> {
            int ev = p.exitValue();
            if (ev != 0) {
                throw new CompilationFailureException("Compiler terminated with exit code " + ev);
            }
        });
    }

    abstract void addArguments(List<String> cmd);

    public void invoke() throws IOException {
        getSource().transferTo(invokerAsDestination());
    }

    void collectError(final Reader reader) throws IOException {
        final String quotedExecPath = Pattern.quote(execPath.toString());
        final Pattern pattern = Pattern.compile("(?:(?:" + quotedExecPath + ": )?(" + LEVEL_PATTERN + "): )?(?:" + quotedExecPath + ": )?(?:([^:]+):(?:(\\d+):(?:(\\d+): )?)?)?(" + LEVEL_PATTERN + "): (.*)");
        final ToolMessageHandler handler = getMessageHandler();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            Matcher matcher;
            StringBuilder b = new StringBuilder();
            ToolMessageHandler.Level level = null;
            int errLine = -1;
            String file = "";
            while ((line = br.readLine()) != null) {
                matcher = pattern.matcher(line.trim());
                if (matcher.matches()) {
                    if (b.length() > 0) {
                        handler.handleMessage(this, level, source.toString(), errLine, -1, b.toString());
                        b.setLength(0);
                    }
                    String levelStr = matcher.group(5);
                    String otherLevelStr = matcher.group(1);
                    if (otherLevelStr != null) {
                        level = getLevel(levelStr).max(getLevel(otherLevelStr));
                    } else {
                        level = getLevel(levelStr);
                    }
                    String lineStr = matcher.group(3);
                    if (lineStr != null) {
                        errLine = Integer.parseInt(lineStr);
                    } else {
                        errLine = -1;
                    }
                    b.append(matcher.group(6));
                } else if (level != null) {
                    b.append('\n').append(line);
                }
            }
            if (b.length() > 0) {
                handler.handleMessage(this, level, source.toString(), errLine, -1, b.toString());
            }
        }
    }

    private ToolMessageHandler.Level getLevel(final String levelStr) {
        switch (levelStr.toLowerCase(Locale.ROOT)) {
            case "note": return ToolMessageHandler.Level.INFO;
            case "warning": return ToolMessageHandler.Level.WARNING;
            default: return ToolMessageHandler.Level.ERROR;
        }
    }
}
