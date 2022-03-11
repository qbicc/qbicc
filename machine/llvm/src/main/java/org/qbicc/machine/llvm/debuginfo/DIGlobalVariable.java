package org.qbicc.machine.llvm.debuginfo;

import java.util.EnumSet;

public interface DIGlobalVariable extends MetadataNode {
    DIGlobalVariable comment(String comment);

    DIGlobalVariable flags(EnumSet<DIFlags> flags);

    DIGlobalVariable isDefinition();

    DIGlobalVariable isLocal();
}
