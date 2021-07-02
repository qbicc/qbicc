package org.qbicc.machine.llvm.debuginfo;

import java.util.EnumSet;

/**
 *
 */
public interface DILocalVariable extends MetadataNode {
    DILocalVariable comment(String comment);

    /**
     * Indicate that the local variable is a method/function argument.
     *
     * @param index the argument index
     * @return this instance
     */
    DILocalVariable argument(int index);

    DILocalVariable flags(EnumSet<DIFlags> flags);
}
