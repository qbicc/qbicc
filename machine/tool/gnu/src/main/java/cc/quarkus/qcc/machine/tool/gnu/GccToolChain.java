package org.qbicc.machine.tool.gnu;

import org.qbicc.machine.tool.CToolChain;

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
