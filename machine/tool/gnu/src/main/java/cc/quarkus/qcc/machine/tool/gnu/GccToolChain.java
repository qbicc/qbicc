package cc.quarkus.qcc.machine.tool.gnu;

import cc.quarkus.qcc.machine.tool.CToolChain;

/**
 *
 */
public interface GccToolChain extends CToolChain {
    default String getImplementationName() {
        return "gnu";
    }

    GnuCCompilerInvoker newCompilerInvoker();

    GnuLinkerInvoker newLinkerInvoker();
}
