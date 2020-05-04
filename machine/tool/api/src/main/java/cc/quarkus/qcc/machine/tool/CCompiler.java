package cc.quarkus.qcc.machine.tool;

/**
 *
 */
public abstract class CCompiler extends Tool {
    protected CCompiler() {
    }

    public String getToolName() {
        return "C Compiler";
    }

    public abstract CompilerInvokerBuilder invocationBuilder();
}
