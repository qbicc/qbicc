package org.qbicc.machine.llvm;

import java.io.BufferedWriter;
import java.io.IOException;

import org.qbicc.machine.llvm.debuginfo.DIBasicType;
import org.qbicc.machine.llvm.debuginfo.DICompileUnit;
import org.qbicc.machine.llvm.debuginfo.DICompositeType;
import org.qbicc.machine.llvm.debuginfo.DIDerivedType;
import org.qbicc.machine.llvm.debuginfo.DIEncoding;
import org.qbicc.machine.llvm.debuginfo.DIExpression;
import org.qbicc.machine.llvm.debuginfo.DIFile;
import org.qbicc.machine.llvm.debuginfo.DIGlobalVariable;
import org.qbicc.machine.llvm.debuginfo.DIGlobalVariableExpression;
import org.qbicc.machine.llvm.debuginfo.DILocalVariable;
import org.qbicc.machine.llvm.debuginfo.DILocation;
import org.qbicc.machine.llvm.debuginfo.DISubprogram;
import org.qbicc.machine.llvm.debuginfo.DISubrange;
import org.qbicc.machine.llvm.debuginfo.DISubroutineType;
import org.qbicc.machine.llvm.debuginfo.DITag;
import org.qbicc.machine.llvm.debuginfo.DebugEmissionKind;
import org.qbicc.machine.llvm.debuginfo.MetadataTuple;
import org.qbicc.machine.llvm.impl.LLVM;

/**
 *
 */
public interface Module {
    DataLayout dataLayout();

    Triple triple();

    void sourceFileName(String path);

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
    DILocalVariable diLocalVariable(String name, LLValue type, LLValue scope, LLValue file, int line, int align);
    DIExpression diExpression();
    DIGlobalVariableExpression diGlobalVariableExpression(LLValue var_, LLValue expr);
    DIGlobalVariable diGlobalVariable(String name, LLValue type, LLValue scope, LLValue file, int line, int align);

    void addFlag(ModuleFlagBehavior behavior, String name, LLValue type, LLValue value);

    void writeTo(BufferedWriter output) throws IOException;

    static Module newModule() {
        return LLVM.newModule();
    }
}
