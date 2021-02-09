package cc.quarkus.qcc.machine.llvm.debuginfo;

import cc.quarkus.qcc.machine.llvm.LLValue;

public interface DIBasicType extends MetadataNode {
    DIBasicType name(String name);
    DIBasicType location(LLValue file, int line);

    DIBasicType comment(String comment);
}
