package org.qbicc.plugin.llvm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.facts.Facts;
import org.qbicc.facts.core.ExecutableReachabilityFacts;
import org.qbicc.graph.InvocationNode;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.llvm.FunctionAttributes;
import org.qbicc.machine.llvm.FunctionDefinition;
import org.qbicc.machine.llvm.Global;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Linkage;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.ModuleFlagBehavior;
import org.qbicc.machine.llvm.RuntimePreemption;
import org.qbicc.machine.llvm.ThreadLocalStorageModel;
import org.qbicc.machine.llvm.Types;
import org.qbicc.machine.llvm.Values;
import org.qbicc.object.Data;
import org.qbicc.object.DataDeclaration;
import org.qbicc.object.Declaration;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.GlobalXtor;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.SectionObject;
import org.qbicc.object.Segment;
import org.qbicc.object.ThreadLocalMode;
import org.qbicc.type.ArrayType;
import org.qbicc.type.StructType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.PointerType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VariadicType;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MemberElement;
import org.qbicc.type.definition.element.MethodElement;

final class LLVMModuleGenerator {
    private final CompilationContext context;
    private final LLVMConfiguration config;
    private final int picLevel;
    private final int pieLevel;

    LLVMModuleGenerator(CompilationContext context, LLVMConfiguration config) {
        this.context = context;
        this.config = config;
        if (config.isPie()) {
            this.picLevel = 2;
            this.pieLevel = 2;
        } else {
            this.picLevel = 0;
            this.pieLevel = 0;
        }
    }

    public void processProgramModule(final ProgramModule programModule, BufferedWriter writer, Path irFile) {
        final Module module = Module.newModule();
        TypeSystem ts = context.getTypeSystem();
        module.dataLayout()
            .byteOrder(ts.getEndianness())
            .pointerSize(ts.getPointerSize() * 8)
            .pointerAlign(ts.getPointerAlignment() * 8)
            .refSize(ts.getReferenceSize() * 8)
            .refAlign(ts.getReferenceAlignment() * 8)
            .int8Align(ts.getUnsignedInteger8Type().getAlign() * 8)
            .int16Align(ts.getUnsignedInteger16Type().getAlign() * 8)
            .int32Align(ts.getUnsignedInteger32Type().getAlign() * 8)
            .int64Align(ts.getUnsignedInteger64Type().getAlign() * 8)
            .float32Align(ts.getFloat32Type().getAlign() * 8)
            .float64Align(ts.getFloat64Type().getAlign() * 8)
            ;
        module.sourceFileName(irFile.toString());
        final LLVMModuleNodeVisitor moduleVisitor = new LLVMModuleNodeVisitor(this, programModule, module, context, config);
        final LLVMModuleDebugInfo debugInfo = new LLVMModuleDebugInfo(programModule, module, context);

        if (picLevel != 0) {
            module.addFlag(ModuleFlagBehavior.Max, "PIC Level", Types.i32, Values.intConstant(picLevel));
        }

        if (pieLevel != 0) {
            module.addFlag(ModuleFlagBehavior.Max, "PIE Level", Types.i32, Values.intConstant(pieLevel));
        }
        final Platform platform = context.getPlatform();

        // declare debug function here
        org.qbicc.machine.llvm.Function decl = module.declare("llvm.dbg.value");
        decl.returns(Types.void_);
        decl.param(Types.metadata).param(Types.metadata).param(Types.metadata);

        // declare global ctors and dtors
        processXtors(programModule.constructors(), "llvm.global_ctors", module, moduleVisitor);
        processXtors(programModule.destructors(), "llvm.global_dtors", module, moduleVisitor);

        for (Declaration item : programModule.declarations()) {
            String name = item.getName();
            Linkage linkage = map(item.getLinkage());
            if (item instanceof FunctionDeclaration fn) {
                decl = module.declare(name).linkage(linkage);
                FunctionType fnType = fn.getValueType();
                decl.returns(moduleVisitor.map(fnType.getReturnType()));
                int cnt = fnType.getParameterCount();
                for (int i = 0; i < cnt; i++) {
                    ValueType type = fnType.getParameterType(i);
                    if (type instanceof VariadicType) {
                        if (i < cnt - 1) {
                            throw new IllegalStateException("Variadic type as non-final parameter type");
                        }
                        decl.variadic();
                    } else {
                        decl.param(moduleVisitor.map(type));
                    }
                }
            } else if (item instanceof DataDeclaration) {
                Global obj = module.global(moduleVisitor.map(item.getValueType())).linkage(Linkage.EXTERNAL);
                ThreadLocalMode tlm = item.getThreadLocalMode();
                if (tlm != null) {
                    obj.threadLocal(map(tlm));
                }
                obj.asGlobal(item.getName());
            }
        }
        for (ModuleSection section : programModule.sections()) {
            String sectionName = section.getName();
            final Segment segment = section.getSection().getSegment();
            for (SectionObject item : section.contents()) {
                String name = item.getName();
                Linkage linkage = map(item.getLinkage());
                if (item instanceof Function fn) {
                    ExecutableElement element = fn.getOriginalElement();
                    if (! Facts.get(context).hadFact(element, ExecutableReachabilityFacts.IS_INVOKED)) {
                        // not reachable; do not emit
                        continue;
                    }
                    MethodBody body = fn.getBody();
                    boolean isExact = item == context.getExactFunction(element);
                    if (body == null) {
                        context.error("Function `%s` has no body", name);
                        continue;
                    }
                    FunctionDefinition functionDefinition = module.define(name).linkage(linkage);
                    LLValue topSubprogram;

                    if (element instanceof MethodElement me) {
                        functionDefinition.comment(me.getEnclosingType().getInternalName()+"."+me.getName()+" "+me.getDescriptor());
                    } else if (element instanceof ConstructorElement ce) {
                        functionDefinition.comment(ce.getEnclosingType().getInternalName()+".<init> "+ce.getDescriptor());
                    }

                    if (isExact) {
                        topSubprogram = debugInfo.getDebugInfoForFunction(element).getSubprogram();
                        functionDefinition.meta("dbg", topSubprogram);
                    } else {
                        topSubprogram = debugInfo.createThunkSubprogram(fn).asRef();
                        functionDefinition.meta("dbg", topSubprogram);
                    }
                    functionDefinition.attribute(FunctionAttributes.framePointer("non-leaf"));
                    functionDefinition.attribute(FunctionAttributes.uwtable);
                    if (config.isStatepointEnabled()) {
                        functionDefinition.gc("statepoint-example");
                    }
                    if (fn.isNoReturn()) {
                        functionDefinition.attribute(FunctionAttributes.noreturn);
                    }

                    LLVMNodeVisitor nodeVisitor = new LLVMNodeVisitor(context, module, debugInfo, topSubprogram, moduleVisitor, fn, functionDefinition);
                    if (! sectionName.equals(CompilationContext.IMPLICIT_SECTION_NAME)) {
                        functionDefinition.section(platform.formatSectionName(segment.toString(), segment.toString(), sectionName));
                    }

                    nodeVisitor.execute();
                } else if (item instanceof Data data) {
                    Literal value = (Literal) data.getValue();
                    Global obj;
                    if (data.isConstant()) {
                        obj = module.constant(moduleVisitor.map(data.getValueType()));
                    } else {
                        obj = module.global(moduleVisitor.map(data.getValueType()));
                    }
                    if (value != null) {
                        obj.value(moduleVisitor.map(value));
                    } else {
                        obj.value(Values.zeroinitializer);
                    }
                    obj.alignment(data.getValueType().getAlign());
                    obj.linkage(linkage);
                    ThreadLocalMode tlm = data.getThreadLocalMode();
                    if (tlm != null) {
                        obj.threadLocal(map(tlm));
                    }
                    if (data.isDsoLocal()) {
                        obj.preemption(RuntimePreemption.LOCAL);
                    }
                    if (! sectionName.equals(CompilationContext.IMPLICIT_SECTION_NAME)) {
                        obj.section(platform.formatSectionName(segment.toString(), segment.toString(), sectionName));
                    }
                    MemberElement element = data.getOriginalElement();
                    if (element != null) {
                        obj.meta("dbg", debugInfo.getDebugInfoForGlobal(data, element));
                    }
                    obj.asGlobal(data.getName());
                } else {
                    throw new IllegalStateException();
                }
            }
        }
        final List<InvocationNode> statePointIds = moduleVisitor.getStatePointIds();
        LLVMInfo.get(context).setStatePointIds(programModule.getTypeDefinition().load(), statePointIds);
        try {
            module.writeTo(writer);
        } catch (IOException e) {
            context.error(Location.builder().setClassInternalName(programModule.getTypeDefinition().getInternalName()).build(), "Failed to emit LLVM output: %s", e.toString());
        }
    }

    private void processXtors(final List<GlobalXtor> xtors, final String xtorName, Module module, LLVMModuleNodeVisitor moduleVisitor) {
        if (! xtors.isEmpty()) {
            TypeSystem ts = context.getTypeSystem();
            LiteralFactory lf = context.getLiteralFactory();
            int xtorSize = 0;
            // this special type has a fixed size and layout
            UnsignedIntegerType u32 = ts.getUnsignedInteger32Type();
            StructType.Member priorityMember = ts.getUnalignedStructTypeMember("priority", u32, 0);
            xtorSize += u32.getSize();
            PointerType voidFnPtrType = ts.getFunctionType(ts.getVoidType(), List.of()).getPointer();
            StructType.Member fnMember = ts.getUnalignedStructTypeMember("fn", voidFnPtrType, xtorSize);
            xtorSize += voidFnPtrType.getSize();
            PointerType u8ptr = ts.getUnsignedInteger8Type().getPointer();
            StructType.Member dataMember = ts.getUnalignedStructTypeMember("data", u8ptr, xtorSize);
            xtorSize += u8ptr.getSize();
            StructType xtorType = ts.getStructType(StructType.Tag.NONE, "xtor_t", xtorSize, 1, () -> List.of(
                priorityMember, fnMember, dataMember
            ));
            // special global
            ArrayType arrayType = ts.getArrayType(xtorType, xtors.size());
            Global global_ctors = module.global(moduleVisitor.map(arrayType));
            global_ctors.appending();
            global_ctors.value(moduleVisitor.map(lf.literalOf(
                arrayType, xtors.stream().map(g -> lf.literalOf(xtorType, Map.of(
                    priorityMember, lf.literalOf(g.getPriority()),
                    fnMember, lf.bitcastLiteral(lf.literalOf(g.getFunction()), voidFnPtrType),
                    dataMember, lf.nullLiteralOfType(u8ptr)
                ))).toList()
            )));
            global_ctors.asGlobal(xtorName);
        }
    }

    public int getLlvmMajor() {
        return config.getMajorVersion();
    }

    Linkage map(org.qbicc.object.Linkage linkage) {
        switch (linkage) {
            case COMMON: return Linkage.COMMON;
            case INTERNAL: return Linkage.INTERNAL;
            case PRIVATE: return Linkage.PRIVATE;
            case WEAK: return Linkage.EXTERN_WEAK;
            case EXTERNAL: return Linkage.EXTERNAL;
            default: throw Assert.impossibleSwitchCase(linkage);
        }
    }

    ThreadLocalStorageModel map(ThreadLocalMode mode) {
        switch (mode) {
            case GENERAL_DYNAMIC: return ThreadLocalStorageModel.GENERAL_DYNAMIC;
            case LOCAL_DYNAMIC: return ThreadLocalStorageModel.LOCAL_DYNAMIC;
            case INITIAL_EXEC: return ThreadLocalStorageModel.INITIAL_EXEC;
            case LOCAL_EXEC: return ThreadLocalStorageModel.LOCAL_EXEC;
            default: throw Assert.impossibleSwitchCase(mode);
        }
    }
}
