package cc.quarkus.qcc.machine.llvm.debuginfo;

import cc.quarkus.qcc.machine.llvm.LLValue;

import java.util.EnumSet;

public interface DICompositeType extends MetadataNode {
    DICompositeType elements(LLValue elements);
    DICompositeType baseType(LLValue baseType);
    DICompositeType name(String name);
    DICompositeType flags(EnumSet<DIFlags> flags);
    DICompositeType location(LLValue file, int line);

    DICompositeType comment(String comment);
}
