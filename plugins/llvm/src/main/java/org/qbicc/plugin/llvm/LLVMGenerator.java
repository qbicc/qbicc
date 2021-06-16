package org.qbicc.plugin.llvm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.schedule.Schedule;
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
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.ProgramObject;
import org.qbicc.object.Section;
import org.qbicc.object.ThreadLocalMode;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VariadicType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.ExecutableElement;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public class LLVMGenerator implements Consumer<CompilationContext>, ValueVisitor<CompilationContext, LLValue> {
    private final int picLevel;
    private final int pieLevel;

    public LLVMGenerator(final int picLevel, final int pieLevel) {
        this.picLevel = picLevel;
        this.pieLevel = pieLevel;
    }

    public void accept(final CompilationContext compilationContext) {
        List<ProgramModule> allProgramModules = compilationContext.getAllProgramModules();
        Iterator<ProgramModule> iterator = allProgramModules.iterator();
        compilationContext.runParallelTask(ctxt -> {
            for (;;) {
                ProgramModule programModule;
                synchronized (iterator) {
                    if (! iterator.hasNext()) {
                        return;
                    }
                    programModule = iterator.next();
                }
                DefinedTypeDefinition def = programModule.getTypeDefinition();
                Path outputFile = ctxt.getOutputFile(def, "ll");
                final Module module = Module.newModule();
                final LLVMModuleNodeVisitor moduleVisitor = new LLVMModuleNodeVisitor(module, ctxt);
                final LLVMModuleDebugInfo debugInfo = new LLVMModuleDebugInfo(module, ctxt);
                final LLVMPseudoIntrinsics pseudoIntrinsics = new LLVMPseudoIntrinsics(module);

                if (picLevel != 0) {
                    module.addFlag(ModuleFlagBehavior.Max, "PIC Level", Types.i32, Values.intConstant(picLevel));
                }

                if (pieLevel != 0) {
                    module.addFlag(ModuleFlagBehavior.Max, "PIE Level", Types.i32, Values.intConstant(pieLevel));
                }

                // declare debug function here
                org.qbicc.machine.llvm.Function decl = module.declare("llvm.dbg.addr");
                decl.returns(Types.void_);
                decl.param(Types.metadata).param(Types.metadata).param(Types.metadata);

                for (Section section : programModule.sections()) {
                    String sectionName = section.getName();
                    for (ProgramObject item : section.contents()) {
                        String name = item.getName();
                        Linkage linkage = map(item.getLinkage());
                        if (item instanceof Function) {
                            ExecutableElement element = ((Function) item).getOriginalElement();
                            MethodBody body = ((Function) item).getBody();
                            boolean isExact = item == ctxt.getExactFunction(element);
                            if (body == null) {
                                ctxt.error("Function `%s` has no body", name);
                                continue;
                            }
                            BasicBlock entryBlock = body.getEntryBlock();
                            FunctionDefinition functionDefinition = module.define(name).linkage(linkage);
                            LLValue topSubprogram = null;

                            if (isExact) {
                                topSubprogram = debugInfo.getDebugInfoForFunction(element).getSubprogram();
                                functionDefinition.meta("dbg", topSubprogram);
                            } else {
                                topSubprogram = debugInfo.createThunkSubprogram((Function) item).asRef();
                                functionDefinition.meta("dbg", topSubprogram);
                            }

                            functionDefinition.attribute(FunctionAttributes.framePointer("non-leaf"));
                            functionDefinition.attribute(FunctionAttributes.uwtable);
                            functionDefinition.gc("statepoint-example");

                            LLVMNodeVisitor nodeVisitor = new LLVMNodeVisitor(ctxt, module, debugInfo, pseudoIntrinsics, topSubprogram, moduleVisitor, Schedule.forMethod(entryBlock), ((Function) item), functionDefinition);
                            if (! sectionName.equals(CompilationContext.IMPLICIT_SECTION_NAME)) {
                                functionDefinition.section(sectionName);
                            }

                            nodeVisitor.execute();
                        } else if (item instanceof FunctionDeclaration) {
                            FunctionDeclaration fn = (FunctionDeclaration) item;
                            decl = module.declare(name).linkage(linkage);
                            FunctionType fnType = fn.getType();
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
                            Global obj = module.global(moduleVisitor.map(item.getType())).linkage(Linkage.EXTERNAL);
                            ThreadLocalMode tlm = item.getThreadLocalMode();
                            if (tlm != null) {
                                obj.threadLocal(map(tlm));
                            }
                            obj.asGlobal(item.getName());
                            if (! sectionName.equals(CompilationContext.IMPLICIT_SECTION_NAME)) {
                                obj.section(sectionName);
                            }
                            if (item.getAddrspace() != 0) {
                                obj.addressSpace(item.getAddrspace());
                            }
                        } else {
                            assert item instanceof Data;
                            Literal value = (Literal) ((Data) item).getValue();
                            Global obj = module.global(moduleVisitor.map(item.getType()));
                            if (value != null) {
                                obj.value(moduleVisitor.map(value));
                            } else {
                                obj.value(Values.zeroinitializer);
                            }
                            obj.alignment(item.getType().getAlign());
                            obj.linkage(linkage);
                            ThreadLocalMode tlm = item.getThreadLocalMode();
                            if (tlm != null) {
                                obj.threadLocal(map(tlm));
                            }
                            if (((Data) item).isDsoLocal()) {
                                obj.preemption(RuntimePreemption.LOCAL);
                            }
                            if (! sectionName.equals(CompilationContext.IMPLICIT_SECTION_NAME)) {
                                obj.section(sectionName);
                            }
                            if (item.getAddrspace() != 0) {
                                obj.addressSpace(item.getAddrspace());
                            }
                            obj.asGlobal(item.getName());
                        }
                    }
                }
                try {
                    Path parent = outputFile.getParent();
                    if (! Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                        module.writeTo(writer);
                    }
                } catch (IOException e) {
                    ctxt.error("Failed to write \"%s\": %s", outputFile, e.getMessage());
                    try {
                        Files.deleteIfExists(outputFile);
                    } catch (IOException e2) {
                        ctxt.warning("Failed to clean \"%s\": %s", outputFile, e.getMessage());
                    }
                }
                LLVMState llvmState = ctxt.computeAttachmentIfAbsent(LLVMState.KEY, LLVMState::new);
                llvmState.addModulePath(outputFile);
            }
        });
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
