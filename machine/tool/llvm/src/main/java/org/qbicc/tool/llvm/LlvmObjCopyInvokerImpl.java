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
public final class LlvmObjCopyInvokerImpl implements LlvmObjCopyInvoker {
    private final LlvmToolChain toolChain;
    private final Path execPath;
    private final List<String> args = new ArrayList<>();
    private Path objectFilePath;
    private ToolMessageHandler messageHandler = ToolMessageHandler.DISCARDING;
    private OutputDestination destination = OutputDestination.discarding();

    LlvmObjCopyInvokerImpl(LlvmToolChain toolChain, Path execPath) {
        this.toolChain = toolChain;
        this.execPath = execPath;
    }

    public void setMessageHandler(ToolMessageHandler messageHandler) {
        this.messageHandler = Assert.checkNotNullParam("messageHandler", messageHandler);
    }

    public ToolMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public Path getPath() {
        return execPath;
    }

    public void invoke() throws IOException {
        InputSource.empty().transferTo(invokerAsDestination());
    }

    public void setObjectFilePath(Path objectFilePath) {
        this.objectFilePath = objectFilePath;
    }

    public void removeSection(String sectionName) {
        args.add("--remove-section=" + sectionName);
    }

    OutputDestination invokerAsDestination() {
        StringBuilder b = new StringBuilder();
        OutputDestination errorHandler = OutputDestination.of(b, StandardCharsets.UTF_8);
        List<String> cmd = new ArrayList<>();
        cmd.add(execPath.toString());
        cmd.addAll(args);
        cmd.add(objectFilePath.toString());
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
                messageHandler.handleMessage(this, level, objectFilePath.toString(), -1, -1, b.toString());
            } else if (ev != 0) {
                messageHandler.handleMessage(this, level, objectFilePath.toString(), -1, -1, "Tool execution failed (exit code = " + ev + ")");
            }
        });
    }

    public LlvmToolChain getTool() {
        return toolChain;
    }
}
