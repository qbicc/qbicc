package cc.quarkus.qcc.tool.llvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import cc.quarkus.qcc.machine.tool.InvokerBuilder;
import cc.quarkus.qcc.machine.tool.Tool;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public class LLCInvocationBuilder extends InvokerBuilder {
    private OutputDestination outputDestination = OutputDestination.discarding();
    private OutputType outputType = OutputType.ASM;

    LLCInvocationBuilder(final Tool tool) {
        super(tool);
    }

    public OutputDestination getOutputDestination() {
        return outputDestination;
    }

    public void setOutputDestination(final OutputDestination outputDestination) {
        Assert.checkNotNullParam("outputDestination", outputDestination);
        this.outputDestination = outputDestination;
    }

    public OutputType getOutputType() {
        return outputType;
    }

    public void setOutputType(final OutputType outputType) {
        Assert.checkNotNullParam("outputType", outputType);
        this.outputType = outputType;
    }

    public OutputDestination build() {
        ProcessBuilder pb = new ProcessBuilder();
        List<String> cmd = new ArrayList<>();
        cmd.add(getTool().getExecutablePath().toString());
        Collections.addAll(cmd, "--filetype=" + outputType.name().toLowerCase(Locale.ROOT));
        pb.command(cmd);
        return OutputDestination.of(pb, OutputDestination.discarding(), outputDestination);
    }

    public enum OutputType {
        ASM,
        OBJ,
        ;
    }
}
