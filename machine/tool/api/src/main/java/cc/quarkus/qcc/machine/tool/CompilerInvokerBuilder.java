package cc.quarkus.qcc.machine.tool;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public abstract class CompilerInvokerBuilder extends InvokerBuilder {
    private Path outputPath;
    private ToolMessageHandler messageHandler;
    private final List<Path> includePaths = new ArrayList<>(0);

    protected CompilerInvokerBuilder(final Tool tool) {
        super(tool);
    }

    public CCompiler getTool() {
        return (CCompiler) super.getTool();
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(final Path outputPath) {
        Assert.checkNotNullParam("outputPath", outputPath);
        this.outputPath = outputPath;
    }

    public ToolMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(final ToolMessageHandler messageHandler) {
        Assert.checkNotNullParam("messageHandler", messageHandler);
        this.messageHandler = messageHandler;
    }

    public void addIncludePath(final Path path) {
        Assert.checkNotNullParam("path", path);
        includePaths.add(path);
    }

    private static final Path[] NO_PATHS = new Path[0];

    public List<Path> getIncludePaths() {
        return List.of(includePaths.toArray(NO_PATHS));
    }

    /**
     * Construct an invoker that will compile a program provided to the returned destination.  The result
     * of the compilation will be passed to the configured message handler and output destination.
     *
     * @return the compiler invoker
     * @see InputSource#transferTo(OutputDestination)
     */
    public abstract OutputDestination build();
}
