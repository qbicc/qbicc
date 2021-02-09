package cc.quarkus.qcc.machine.llvm.debuginfo;

import cc.quarkus.qcc.machine.llvm.LLValue;

import java.util.EnumSet;

public interface DIDerivedType extends MetadataNode {
    DIDerivedType baseType(LLValue baseType);
    DIDerivedType name(String name);
    DIDerivedType flags(EnumSet<DIFlags> flags);
    DIDerivedType offset(long offset);
    DIDerivedType location(LLValue file, int line);

    DIDerivedType comment(String comment);
}
