package cc.quarkus.qcc.machine.tool;

/**
 *
 */
public interface CToolChain extends Tool {

    default String getToolName() {
        return "C Tool Chain";
    }

    CCompilerInvoker newCompilerInvoker();

    LinkerInvoker newLinkerInvoker();
}
