package cc.quarkus.qcc.machine.tool.gnu;

import cc.quarkus.qcc.machine.tool.CCompiler;

/**
 *
 */
public interface GnuCCompiler extends CCompiler {
    default String getImplementationName() {
        return "gnu";
    }

    default String getProgramName() {
        return "gcc";
    }

    GnuCCompilerInvoker newCompilerInvoker();

    GnuLinkerInvoker newLinkerInvoker();
}
