package cc.quarkus.qcc.plugin.llvm;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.ModuleFlagBehavior;
import cc.quarkus.qcc.machine.llvm.Types;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.debuginfo.DICompositeType;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIEncoding;
import cc.quarkus.qcc.machine.llvm.debuginfo.DIFlags;
import cc.quarkus.qcc.machine.llvm.debuginfo.DISubprogram;
import cc.quarkus.qcc.machine.llvm.debuginfo.DITag;
import cc.quarkus.qcc.machine.llvm.debuginfo.DebugEmissionKind;
import cc.quarkus.qcc.machine.llvm.debuginfo.MetadataNode;
import cc.quarkus.qcc.machine.llvm.debuginfo.MetadataTuple;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.plugin.layout.Layout;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.PhysicalObjectType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.TypeType;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.VoidType;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.Element;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FunctionElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

final class LLVMModuleDebugInfo {
    private final Module module;
    private final CompilationContext ctxt;

    private final LLValue diCompileUnit;
    private final Map<ExecutableElement, MethodDebugInfo> methods = new HashMap<>();
    private final Map<Type, LLValue> types = new HashMap<>();

    LLVMModuleDebugInfo(final Module module, final CompilationContext ctxt) {
        this.module = module;
        this.ctxt = ctxt;

        module.addFlag(ModuleFlagBehavior.Warning, "Debug Info Version", Types.i32, Values.intConstant(3));
        module.addFlag(ModuleFlagBehavior.Warning, "Dwarf Version", Types.i32, Values.intConstant(4));

        // TODO Generate correct filenames
        diCompileUnit = module.diCompileUnit("DW_LANG_Java", module.diFile("<stdin>", "").asRef(), DebugEmissionKind.FullDebug).asRef();
    }

    private String getFriendlyName(final ExecutableElement element) {
        StringBuilder b = new StringBuilder();

        b.append(element.getEnclosingType().getInternalName().replace('/', '.'));
        b.append('.');

        if (element instanceof InitializerElement) {
            b.append("<clinit>");
        } else if (element instanceof ConstructorElement) {
            b.append("<init>");
        } else if (element instanceof MethodElement) {
            b.append(((MethodElement) element).getName());
        } else if (element instanceof FunctionElement) {
            b.append(((FunctionElement) element).getName());
        } else {
            throw new UnsupportedOperationException("Unrecognized element " + element.toString());
        }

        return b.toString();
    }

    private LLValue createSourceFile(final Element element) {
        String sourceFileName = element.getSourceFileName();
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

        return module.diFile(sourceFileName, sourceFileDirectory).asRef();
    }

    private MethodDebugInfo createDebugInfoForFunction(final ExecutableElement element) {
        LLValue type = getType(element.getType());
        int line = element.getMinimumLineNumber();

        LLValue diSubprogram = module.diSubprogram(getFriendlyName(element), type, diCompileUnit)
                .location(createSourceFile(element), line, line)
                .linkageName(ctxt.getExactFunction(element).getName())
                .asRef();

        MethodDebugInfo debugInfo = new MethodDebugInfo(diSubprogram);

        methods.put(element, debugInfo);
        return debugInfo;
    }

    public DISubprogram createThunkSubprogram(final Function function) {
        LLValue type = getType(function.getType());
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

    private MetadataTuple populateCompoundType(final CompoundType type, final DICompositeType diType) {
        MetadataTuple elements = module.metadataTuple();
        diType.elements(elements.asRef());

        for (CompoundType.Member m : type.getMembers()) {
            ValueType mt = m.getType();
            elements.elem(null,
                module.diDerivedType(DITag.Member, mt.getSize() * 8, mt.getAlign() * 8)
                    .name(m.getName())
                    .baseType(getType(mt))
                    .offset((long)m.getOffset() * 8)
                    .asRef()
            );
        }

        return elements;
    }

    private LLValue createCompoundType(final CompoundType type) {
        DICompositeType diType = module.diCompositeType(DITag.StructureType, type.getSize() * 8, type.getAlign() * 8)
            .name(type.toFriendlyString());
        LLValue result = registerType(type, diType);

        populateCompoundType(type, diType);
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

    private LLValue createPhysicalObjectType(final PhysicalObjectType type, final CompoundType compoundType) {
        DICompositeType diType = module.diCompositeType(DITag.StructureType, compoundType.getSize() * 8, compoundType.getAlign() * 8)
            .name(type.toFriendlyString());
        LLValue result = registerType(type, diType);

        MetadataTuple elements = populateCompoundType(compoundType, diType);

        if (type.hasSuperClass()) {
            ClassObjectType superType = type.getSuperClassType();
            CompoundType superCompoundType = Layout.get(ctxt).getInstanceLayoutInfo(superType.getDefinition()).getCompoundType();
            LLValue superDiType = getType(superType);

            elements.elem(null,
                module.diDerivedType(DITag.Inheritance, superCompoundType.getSize() * 8, superCompoundType.getAlign() * 8)
                    .baseType(superDiType)
                    .offset(0)
                    .asRef()
            );
        }

        return result;
    }

    private LLValue createClassObjectType(final ClassObjectType type) {
        CompoundType compoundType = Layout.get(ctxt).getInstanceLayoutInfo(type.getDefinition()).getCompoundType();
        return createPhysicalObjectType(type, compoundType);
    }

    private LLValue createArrayObjectType(final ArrayObjectType type) {
        CompoundType compoundType = Layout.get(ctxt).getInstanceLayoutInfo(Layout.get(ctxt).getArrayContentField(type).getEnclosingType()).getCompoundType();
        return createPhysicalObjectType(type, compoundType);
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
        } else if (type instanceof CompoundType) {
            return createCompoundType((CompoundType) type);
        } else if (type instanceof FloatType) {
            return createBasicType((FloatType) type, DIEncoding.Float);
        } else if (type instanceof FunctionType) {
            return createFunctionType((FunctionType) type);
        } else if (type instanceof PointerType) {
            return createPointerType((PointerType) type, ((PointerType) type).getPointeeType());
        } else if (type instanceof ReferenceType) {
            return createPointerType((ReferenceType) type, ((ReferenceType) type).getUpperBound());
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
