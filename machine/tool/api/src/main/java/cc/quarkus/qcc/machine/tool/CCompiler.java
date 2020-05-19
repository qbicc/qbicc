package cc.quarkus.qcc.machine.tool;

/**
 *
 */
public interface CCompiler extends Tool {

    default String getToolName() {
        return "C Compiler";
    }

    CCompilerInvoker newCompilerInvoker();

    LinkerInvoker newLinkerInvoker();
}
