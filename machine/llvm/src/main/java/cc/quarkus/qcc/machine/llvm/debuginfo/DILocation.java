package org.qbicc.machine.llvm.debuginfo;

/**
 *
 */
public interface DILocation extends MetadataNode {
    DILocation comment(String comment);

    DILocation distinct(boolean distinct);
}
