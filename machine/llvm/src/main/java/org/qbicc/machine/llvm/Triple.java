package org.qbicc.machine.llvm;

/**
 *
 */
public interface Triple {
    Triple arch(String arch);

    Triple vendor(String vendor);

    Triple os(String os);

    Triple env(String env);
}
