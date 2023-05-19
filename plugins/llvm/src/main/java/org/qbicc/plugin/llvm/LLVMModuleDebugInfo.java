package org.qbicc.plugin.llvm;

import static org.qbicc.machine.llvm.Values.metadataString;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.ModuleFlagBehavior;
import org.qbicc.machine.llvm.Types;
import org.qbicc.machine.llvm.Values;
import org.qbicc.machine.llvm.debuginfo.DICompositeType;
import org.qbicc.machine.llvm.debuginfo.DIDerivedType;
import org.qbicc.machine.llvm.debuginfo.DIEncoding;
import org.qbicc.machine.llvm.debuginfo.DIFlags;
import org.qbicc.machine.llvm.debuginfo.DISubprogram;
import org.qbicc.machine.llvm.debuginfo.DITag;
import org.qbicc.machine.llvm.debuginfo.DebugEmissionKind;
import org.qbicc.machine.llvm.debuginfo.MetadataNode;
import org.qbicc.machine.llvm.debuginfo.MetadataTuple;
import org.qbicc.object.Data;
import org.qbicc.object.Function;
import org.qbicc.object.ProgramModule;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.StructType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.MethodType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.Type;
import org.qbicc.type.TypeType;
import org.qbicc.type.UnionType;
import org.qbicc.type.UnresolvedType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.Element;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MemberElement;
import org.qbicc.type.definition.element.MethodElement;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class LLVMModuleDebugInfo {
    private final Module module;
    private final CompilationContext ctxt;

    private final LLValue diCompileUnit;
    private final Map<ExecutableElement, MethodDebugInfo> methods = new HashMap<>();
    private final Map<Type, LLValue> types = new HashMap<>();
    private final Map<LocationKey, LLValue> locations = new HashMap<>();
    private final Map<String, LLValue> files = new HashMap<>();
    private final LLValue file;
    private final MetadataTuple globals;

    LLVMModuleDebugInfo(ProgramModule programModule, final Module module, final CompilationContext ctxt) {
        this.module = module;
        this.ctxt = ctxt;

        module.addFlag(ModuleFlagBehavior.Warning, "Debug Info Version", Types.i32, Values.intConstant(3));
        module.addFlag(ModuleFlagBehavior.Warning, "Dwarf Version", Types.i32, Values.intConstant(4));
        module.metadataTuple("llvm.ident").elem(null, module.metadataTuple().elem(null, metadataString("qbicc")).asRef());

        final DefinedTypeDefinition typeDefinition = programModule.getTypeDefinition();
        final String fullPath = typeDefinition.getInternalName() + ".java";
        final int idx = fullPath.lastIndexOf('/');
        final String dirName, fileName;
        if (idx == -1) {
            dirName = "";
            fileName = fullPath;
        } else {
            dirName = fullPath.substring(0, idx);
            fileName = fullPath.substring(idx + 1);
        }

        file = module.diFile(fileName, dirName).asRef();
        globals = module.metadataTuple();
        // todo: change back to DW_LANG_Java as soon as debugger support is solidified
        diCompileUnit = module.diCompileUnit("DW_LANG_C_plus_plus", file, DebugEmissionKind.FullDebug).producer("qbicc").globals(globals.asRef()).asRef();
    }

    private String getFriendlyName(final ExecutableElement element) {
        if (element instanceof FunctionElement fe) {
            return fe.getName();
        }
        StringBuilder b = new StringBuilder();

        b.append(element.getEnclosingType().getInternalName().replace('/', '.'));
        b.append('.');

        if (element instanceof InitializerElement) {
            b.append("<clinit>");
        } else if (element instanceof ConstructorElement) {
            b.append("<init>");
        } else if (element instanceof MethodElement) {
            b.append(((MethodElement) element).getName());
        } else {
            throw new UnsupportedOperationException("Unrecognized element " + element.toString());
        }

        return b.toString();
    }

    public LLValue createSourceFile(final Element element) {
        String sourceFileNameFull = element.getSourceFileName();

        LLValue file = files.get(sourceFileNameFull);
        if (file != null) {
            return file;
        }

        String sourceFileName = sourceFileNameFull;
        String sourceFileDirectory = "";

        if (sourceFileName != null) {
            String typeName = element.getEnclosingType().getInternalName();
            int typeNamePackageEnd = typeName.lastIndexOf('/');

            if (typeNamePackageEnd != -1) {
                sourceFileDirectory = typeName.substring(0, typeNamePackageEnd);
            }
        } else {
            sourceFileName = "<unknown>";
        }

        file = module.diFile(sourceFileName, sourceFileDirectory).asRef();
        files.put(sourceFileNameFull, file);
        return file;
    }

    private MethodDebugInfo createDebugInfoForFunction(final ExecutableElement element) {
        Function exactFunction = ctxt.getExactFunctionIfExists(element);
        LLValue type = getType(exactFunction.getValueType());
        int line = element.getMinimumLineNumber();

        DISubprogram diSubprogram = module.diSubprogram(getFriendlyName(element), type, diCompileUnit)
            .location(createSourceFile(element), line, line);

        if (exactFunction != null) {
            diSubprogram.linkageName(exactFunction.getName());
        }

        MethodDebugInfo debugInfo = new MethodDebugInfo(diSubprogram.asRef());

        methods.put(element, debugInfo);
        return debugInfo;
    }

    public DISubprogram createThunkSubprogram(final Function function) {
        LLValue type = getType(function.getSymbolType());
        int line = function.getOriginalElement().getMinimumLineNumber();

        return module.diSubprogram(function.getName(), type, diCompileUnit)
                .location(createSourceFile(function.getOriginalElement()), line, line)
                .linkageName(function.getName());
    }

    public MethodDebugInfo getDebugInfoForFunction(final ExecutableElement element) {
        MethodDebugInfo debugInfo = methods.get(element);

        if (debugInfo == null) {
            debugInfo = createDebugInfoForFunction(element);
        }

        return debugInfo;
    }

    private LLValue registerType(final Type type, final MetadataNode debugInfo) {
        LLValue ref = debugInfo.asRef();

        types.put(type, ref);
        return ref;
    }

    private LLValue createBasicType(final ValueType type, final DIEncoding encoding) {
        return registerType(
            type,
            module.diBasicType(encoding, type.getSize() * 8, type.getAlign() * 8).name(type.toFriendlyString())
        );
    }

    private LLValue createPointerType(final ValueType type, final Type toType) {
        LLValue toTypeDbg = null;

        if (!(toType instanceof VoidType))
            toTypeDbg = getType(toType);

        return registerType(
            type,
            module.diDerivedType(DITag.PointerType, type.getSize() * 8, type.getAlign() * 8).baseType(toTypeDbg)
        );
    }

    private MetadataTuple populateStructType(final StructType type, final DICompositeType diType, Set<StructType.Member> excludeMembers) {
        MetadataTuple elements = module.metadataTuple();
        diType.elements(elements.asRef());

        for (StructType.Member m : type.getMembers()) {
            if (! excludeMembers.contains(m)) {
                ValueType mt = m.getType();
                elements.elem(null,
                    module.diDerivedType(DITag.Member, mt.getSize() * 8, mt.getAlign() * 8)
                        .name(m.getName())
                        .baseType(getType(mt))
                        .offset((long)m.getOffset() * 8)
                        .asRef()
                );
            }
        }

        return elements;
    }

    private LLValue createStructType(final StructType type) {
        DICompositeType diType = module.diCompositeType(DITag.StructureType, type.getSize() * 8, type.getAlign() * 8)
            .name(type.toFriendlyString());
        LLValue result = registerType(type, diType);

        populateStructType(type, diType, Set.of());
        return result;
    }

    private LLValue createUnionType(final UnionType type) {
        final DICompositeType diType = module.diCompositeType(DITag.UnionType, type.getSize() * 8, type.getAlign() * 8).name(type.toFriendlyString());
        LLValue result = registerType(type, diType);

        final MetadataTuple elements = module.metadataTuple();
        diType.elements(elements.asRef());

        for (UnionType.Member member : type.getMembers()) {
            final ValueType mt = member.getType();
            elements.elem(null,
                module.diDerivedType(DITag.Member, mt.getSize() * 8, mt.getAlign() * 8)
                    .name(member.getName())
                    .baseType(getType(mt))
                    .asRef()
            );
        }

        return result;
    }

    private LLValue createFunctionType(final FunctionType type) {
        MetadataTuple types = module.metadataTuple();
        LLValue result = registerType(
            type,
            module.diSubroutineType(types.asRef())
        );

        if (type.getReturnType() instanceof VoidType) {
            types.elem(null, null);
        } else {
            types.elem(null, getType(type.getReturnType()));
        }

        for (int i = 0; i < type.getParameterCount(); i++) {
            types.elem(null, getType(type.getParameterType(i)));
        }

        return result;
    }

    private LLValue createPhysicalObjectType(final PhysicalObjectType type, LayoutInfo instanceLayoutInfo, final StructType structType) {
        DICompositeType diType = module.diCompositeType(DITag.ClassType, structType.getSize() * 8, structType.getAlign() * 8)
            .name(type.toFriendlyString());
        if (!(type instanceof ClassObjectType cot) || cot.getDefinition().isPublic()) {
            diType.flags(EnumSet.of(DIFlags.Public));
        }
        LLValue result = registerType(type, diType);

        Set<StructType.Member> exclude;
        if (type.hasSuperClass()) {
            // the supertype's fields are going to be included in the derived type part
            ClassObjectType superType = type.getSuperClassType();
            StructType superStructType = Layout.get(ctxt).getInstanceLayoutInfo(superType.getDefinition()).getStructType();
            exclude = Set.of(superStructType.getMembers().toArray(StructType.Member[]::new));
        } else {
            exclude = Set.of();
        }

        MetadataTuple elements = module.metadataTuple();
        diType.elements(elements.asRef());

        for (FieldElement fieldElement : instanceLayoutInfo.getFieldsMap().keySet()) {
            StructType.Member member = instanceLayoutInfo.getMember(fieldElement);
            if (exclude.contains(member)) {
                continue;
            }
            ValueType mt = fieldElement.getType();
            DIFlags access = fieldElement.isPublic() ? DIFlags.Public : fieldElement.isPrivate() ? DIFlags.Private : fieldElement.isProtected() ? DIFlags.Protected : null;
            DIDerivedType derivedType = module.diDerivedType(DITag.Member, mt.getSize() * 8, mt.getAlign() * 8)
                .name(member.getName())
                .baseType(getType(mt))
                .offset((long) member.getOffset() * 8);
            if (access != null) {
                derivedType.flags(EnumSet.of(access));
            }
            elements.elem(null, derivedType.asRef());
        }

        if (type.hasSuperClass()) {
            ClassObjectType superType = type.getSuperClassType();
            StructType superStructType = Layout.get(ctxt).getInstanceLayoutInfo(superType.getDefinition()).getStructType();
            LLValue superDiType = getType(superType);

            elements.elem(null,
                module.diDerivedType(DITag.Inheritance, superStructType.getSize() * 8, superStructType.getAlign() * 8)
                    .baseType(superDiType)
                    .offset(0)
                    .flags(EnumSet.of(DIFlags.Public, DIFlags.SingleInheritance))
                    .asRef()
            );
        }

        return result;
    }

    private LLValue createClassObjectType(final ClassObjectType type) {
        LayoutInfo instanceLayoutInfo = Layout.get(ctxt).getInstanceLayoutInfo(type.getDefinition());
        StructType structType = instanceLayoutInfo.getStructType();
        return createPhysicalObjectType(type, instanceLayoutInfo, structType);
    }

    private LLValue createArrayObjectType(final ArrayObjectType type) {
        LayoutInfo instanceLayoutInfo = Layout.get(ctxt).getInstanceLayoutInfo(CoreClasses.get(ctxt).getArrayContentField(type).getEnclosingType());
        StructType structType = instanceLayoutInfo.getStructType();
        return createPhysicalObjectType(type, instanceLayoutInfo, structType);
    }

    private LLValue createArrayType(final ArrayType type) {
        DICompositeType derivedType = module.diCompositeType(DITag.ArrayType, type.getSize() * 8, type.getAlign() * 8);
        LLValue result = registerType(type, derivedType);

        derivedType.baseType(getType(type.getElementType()))
            .elements(module.metadataTuple().elem(null, module.diSubrange(type.getElementCount()).asRef()).asRef());
        return result;
    }

    private LLValue createFallbackType(final Type type) {
        long size = 0;
        int align = 1;

        if (type instanceof ValueType) {
            size = ((ValueType) type).getSize();
            align = ((ValueType) type).getAlign();
        }

        return registerType(
            type,
            module.diCompositeType(DITag.StructureType, size, align).name(type.toFriendlyString()).flags(EnumSet.of(DIFlags.FwdDecl))
        );
    }

    private LLValue createType(final Type type) {
        if (type instanceof ArrayType) {
            return createArrayType((ArrayType) type);
        } else if (type instanceof ArrayObjectType) {
            return createArrayObjectType((ArrayObjectType) type);
        } else if (type instanceof BooleanType) {
            return createBasicType((BooleanType) type, DIEncoding.Boolean);
        } else if (type instanceof ClassObjectType) {
            return createClassObjectType((ClassObjectType) type);
        } else if (type instanceof StructType) {
            return createStructType((StructType) type);
        } else if (type instanceof UnionType ut) {
            return createUnionType(ut);
        } else if (type instanceof FloatType) {
            return createBasicType((FloatType) type, DIEncoding.Float);
        } else if (type instanceof FunctionType) {
            return createFunctionType((FunctionType) type);
        } else if (type instanceof MethodType mt) {
            return createType(ctxt.getFunctionTypeForInvokableType(mt));
        } else if (type instanceof PointerType) {
            return createPointerType((PointerType) type, ((PointerType) type).getPointeeType());
        } else if (type instanceof ReferenceType) {
            return createPointerType((ReferenceType) type, ((ReferenceType) type).getUpperBound());
        } else if (type instanceof UnresolvedType ut) {
            return createPointerType(ut, ut.getTypeSystem().getVoidType().getPointer());
        } else if (type instanceof SignedIntegerType) {
            return createBasicType((SignedIntegerType) type, DIEncoding.Signed);
        } else if (type instanceof TypeType) {
            return createBasicType((TypeType) type, DIEncoding.Unsigned);
        } else if (type instanceof UnsignedIntegerType) {
            return createBasicType((UnsignedIntegerType) type, DIEncoding.Unsigned);
        } else {
            ctxt.warning("LLVM: Unhandled type %s for debug info generation", type.toFriendlyString());
            return createFallbackType(type);
        }
    }

    public LLValue getType(final Type type) {
        LLValue debugInfo = types.get(type);

        if (debugInfo == null) {
            debugInfo = createType(type);
        }

        return debugInfo;
    }

    public LLValue createDeduplicatedLocation(int line, int column, LLValue scope, LLValue inlinedAt) {
        Assert.checkNotNullParam("scope", scope);

        LocationKey key = new LocationKey(line, column, scope, inlinedAt);
        LLValue location = locations.get(key);
        if (location != null) {
            return location;
        }

        location = module.diLocation(line, column, scope, inlinedAt).asRef();
        locations.put(key, location);
        return location;
    }

    public LLValue getDebugInfoForGlobal(final Data data, final MemberElement element) {
        String name = data.getName();
        ValueType valueType;
        if (element instanceof FieldElement fe) {
            valueType = fe.getType();
        } else {
            valueType = data.getValueType();
        }
        LLValue type = getType(valueType);
        // todo: the alignment should derive from the member/object
        int align = valueType.getAlign();
        LLValue sourceFile = createSourceFile(element);
        LLValue var_ = module.diGlobalVariable(name, type, diCompileUnit, sourceFile, 1, align).isDefinition().asRef();
        LLValue expr = module.diGlobalVariableExpression(var_, Values.diExpression().asValue()).asRef();
        globals.elem(null, expr);
        return expr;
    }

    final static class LocationKey {
        private final int line;
        private final int column;
        private final LLValue scope;
        private final LLValue inlinedAt;

        LocationKey(final int line, final int column, final LLValue scope, final LLValue inlinedAt) {
            this.line = line;
            this.column = column;
            this.scope = scope;
            this.inlinedAt = inlinedAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LocationKey that = (LocationKey) o;
            return line == that.line && column == that.column && scope.equals(that.scope) && Objects.equals(inlinedAt, that.inlinedAt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(line, column, scope, inlinedAt);
        }
    }

    final static class MethodDebugInfo {
        private final LLValue subprogram;

        MethodDebugInfo(final LLValue diSubprogram) {
            this.subprogram = diSubprogram;
        }

        public LLValue getSubprogram() {
            return subprogram;
        }

        public LLValue getScope(int bci) {
            // TODO Once variable debug info is available, choose the correct DILexicalBlock
            return subprogram;
        }
    }
}
