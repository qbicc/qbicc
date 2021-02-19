package cc.quarkus.qcc.machine.llvm;

import java.io.BufferedWriter;
import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.debuginfo.DIBasicType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DICompileUnit;
import cc.quarkus.qcc.machine.llvm.debuginfo.DICompositeType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIDerivedType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIEncoding;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIFile;
import cc.quarkus.qcc.machine.llvm.debuginfo.DILocation;
import cc.quarkus.qcc.machine.llvm.debuginfo.DISubprogram;
import cc.quarkus.qcc.machine.llvm.debuginfo.DISubrange;
import cc.quarkus.qcc.machine.llvm.debuginfo.DISubroutineType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DITag;
import cc.quarkus.qcc.machine.llvm.debuginfo.DebugEmissionKind;
import cc.quarkus.qcc.machine.llvm.debuginfo.MetadataTuple;
import cc.quarkus.qcc.machine.llvm.impl.LLVM;

/**
 *
 */
public interface Module {
    // todo: metadata goes at the end for definitions
    FunctionDefinition define(String name);

    // todo: metadata goes after `declare` for declarations
    Function declare(String name);

    Global global(LLValue type);

    Global constant(LLValue type);

    IdentifiedType identifiedType();
    IdentifiedType identifiedType(String name);

    MetadataTuple metadataTuple();
    MetadataTuple metadataTuple(String name);

    DICompileUnit diCompileUnit(String language, LLValue file, DebugEmissionKind emissionKind);
    DIFile diFile(String filename, String directory);
    DILocation diLocation(int line, int column, LLValue scope, LLValue inlinedAt);
    DISubprogram diSubprogram(String name, LLValue type, LLValue unit);
    DISubrange diSubrange(long count);
    DIBasicType diBasicType(DIEncoding encoding, long size, int align);
    DIDerivedType diDerivedType(DITag tag, long size, int align);
    DICompositeType diCompositeType(DITag tag, long size, int align);
    DISubroutineType diSubroutineType(LLValue types);

    void addFlag(ModuleFlagBehavior behavior, String name, LLValue type, LLValue value);

    void writeTo(BufferedWriter output) throws IOException;

    static Module newModule() {
        return LLVM.newModule();
    }
}
