package org.qbicc.tool.llvm;

/**
 *
 */
public enum OptPass {
    O0("O0"),
    O1("O1"),
    O2("O2"),
    O3("O3"),
    Os("Os"),
    Oz("Oz"),
    RewriteStatepointsForGc("rewrite-statepoints-for-gc"),
    AlwaysInline("always-inline");

    public final String name;

    OptPass(String name) {
        this.name = name;
    }
}
