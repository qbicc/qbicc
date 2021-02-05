package cc.quarkus.qcc.plugin.llvm;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Types;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.debuginfo.DISubprogram;
import cc.quarkus.qcc.machine.llvm.debuginfo.DebugEmissionKind;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.Element;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FunctionElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

import java.util.HashMap;
import java.util.Map;

final class LLVMModuleDebugInfo {
    private final Module module;
    private final CompilationContext ctxt;

    private final LLValue diCompileUnit;
    private final Map<ExecutableElement, MethodDebugInfo> methods = new HashMap<>();

    LLVMModuleDebugInfo(final Module module, final CompilationContext ctxt) {
        this.module = module;
        this.ctxt = ctxt;

        final LLValue diVersionTuple = module.metadataTuple()
                .elem(Types.i32, Values.intConstant(2))
                .elem(null, Values.metadataString("Debug Info Version"))
                .elem(Types.i32, Values.intConstant(3))
                .asRef();
        final LLValue dwarfVersionTuple = module.metadataTuple()
                .elem(Types.i32, Values.intConstant(2))
                .elem(null, Values.metadataString("Dwarf Version"))
                .elem(Types.i32, Values.intConstant(4))
                .asRef();

        module.metadataTuple("llvm.module.flags").elem(null, diVersionTuple).elem(null, dwarfVersionTuple);

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
        // TODO Generate correct subroutine types
        LLValue type = module.diSubroutineType(module.metadataTuple().elem(null, null).asRef()).asRef();
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
        // TODO Generate correct subroutine types
        LLValue type = module.diSubroutineType(module.metadataTuple().elem(null, null).asRef()).asRef();
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
