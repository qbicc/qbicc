package cc.quarkus.qcc.machine.tool;

/**
 *
 */
public abstract class CompilerInvocationBuilder<P> extends InvocationBuilder<P, CompilationResult> {
    protected CompilerInvocationBuilder(final Tool tool) {
        super(tool);
    }

    public CCompiler getTool() {
        return (CCompiler) super.getTool();
    }

    public InputSource getInputSource() {
        return super.getInputSource();
    }

    public CompilerInvocationBuilder<P> setInputSource(final InputSource inputSource) {
        super.setInputSource(inputSource);
        return this;
    }

    public CompilationResult invoke() throws ToolExecutionFailureException {
        return super.invoke();
    }
}
