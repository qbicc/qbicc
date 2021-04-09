package org.qbicc.machine.llvm.debuginfo;

import org.qbicc.machine.llvm.LLValue;

public interface DIBasicType extends MetadataNode {
    DIBasicType name(String name);
    DIBasicType location(LLValue file, int line);

    DIBasicType comment(String comment);
}
