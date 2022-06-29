package org.qbicc.tool.llvm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;

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
        StringBuilder b = new StringBuilder();
        OutputDestination errorHandler = OutputDestination.of(b, StandardCharsets.UTF_8);
        List<String> cmd = new ArrayList<>();
        cmd.add(execPath.toString());
        addArguments(cmd);
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(cmd);
        pb.environment().put("LC_ALL", "C");
        pb.environment().put("LANG", "C");
        return OutputDestination.of(pb, errorHandler, destination, p -> {
            int ev = p.exitValue();
            ToolMessageHandler.Level level = ev == 0 ? ToolMessageHandler.Level.WARNING : ToolMessageHandler.Level.ERROR;
            if (! b.isEmpty()) {
                if (ev != 0) {
                    b.append("\n(exit code = ").append(ev).append(')');
                }
                messageHandler.handleMessage(this, level, source.toString(), -1, -1, b.toString());
            } else if (ev != 0) {
                messageHandler.handleMessage(this, level, source.toString(), -1, -1, "Tool execution failed (exit code = " + ev + ")");
            }
        });
    }

    abstract void addArguments(List<String> cmd);

    public void invoke() throws IOException {
        getSource().transferTo(invokerAsDestination());
    }
}
