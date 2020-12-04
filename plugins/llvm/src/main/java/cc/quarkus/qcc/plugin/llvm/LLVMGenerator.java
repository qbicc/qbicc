package cc.quarkus.qcc.plugin.llvm;

import static cc.quarkus.qcc.machine.llvm.Types.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Types;
import cc.quarkus.qcc.object.Data;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.FunctionDeclaration;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.object.ProgramObject;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.VoidType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodBody;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public class LLVMGenerator implements Consumer<CompilationContext> {

    final Map<Type, LLValue> types = new HashMap<>();

    public void accept(final CompilationContext ctxt) {
        for (ProgramModule programModule : ctxt.getAllProgramModules()) {
            DefinedTypeDefinition def = programModule.getTypeDefinition();
            Path path = ctxt.getOutputDirectory(def);
            final Module module = Module.newModule();
            for (Section section : programModule.sections()) {
                String sectionName = section.getName();
                for (ProgramObject item : section.contents()) {
                    String name = item.getName();
                    if (item instanceof Function) {
                        MethodBody body = ((Function) item).getBody();
                        if (body == null) {
                            ctxt.error("Function `%s` has no body", name);
                            continue;
                        }
                        BasicBlock entryBlock = body.getEntryBlock();
                        FunctionDefinition functionDefinition = module.define(name);
                        LLVMNodeVisitor nodeVisitor = new LLVMNodeVisitor(ctxt, this, Schedule.forMethod(entryBlock), ((Function) item), functionDefinition);
                        if (! sectionName.equals(CompilationContext.IMPLICIT_SECTION_NAME)) {
                            functionDefinition.section(sectionName);
                        }

                        nodeVisitor.execute();
                    } else if (item instanceof FunctionDeclaration) {
                        FunctionDeclaration fn = (FunctionDeclaration) item;
                        cc.quarkus.qcc.machine.llvm.Function decl = module.declare(name);
                        FunctionType fnType = fn.getType();
                        decl.returns(map(fnType.getReturnType()));
                        int cnt = fnType.getParameterCount();
                        for (int i = 0; i < cnt; i ++) {
                            decl.param(map(fnType.getParameterType(i)));
                        }
                    } else {
                        assert item instanceof Data;
                        // todo: support data output
                    }
                }
            }
            Path outputFile = path.resolve("output.ll");
            try {
                Files.createDirectories(outputFile.getParent());
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
    }

    LLValue map(Type type) {
        LLValue res = types.get(type);
        if (res != null) {
            return res;
        }
        if (type instanceof VoidType) {
            res = void_;
        } else if (type instanceof FunctionType) {
            FunctionType fnType = (FunctionType) type;
            int cnt = fnType.getParameterCount();
            List<LLValue> argTypes = cnt == 0 ? List.of() : new ArrayList<>(cnt);
            for (int i = 0; i < cnt; i ++) {
                argTypes.add(map(fnType.getParameterType(i)));
            }
            res = Types.function(map(fnType.getReturnType()), argTypes);
        } else if (type instanceof BooleanType) {
            // todo: sometimes it's one byte instead
            res = i1;
        } else if (type instanceof IntegerType) {
            // LLVM doesn't really care about signedness
            int bytes = (int) ((IntegerType) type).getSize();
            if (bytes == 1) {
                res = i8;
            } else if (bytes == 2) {
                res = i16;
            } else if (bytes == 4) {
                res = i32;
            } else if (bytes == 8) {
                res = i64;
            } else {
                throw Assert.unreachableCode();
            }
        } else if (type instanceof FloatType) {
            int bytes = (int) ((FloatType) type).getSize();
            if (bytes == 4) {
                res = float32;
            } else if (bytes == 8) {
                res = float64;
            } else {
                throw Assert.unreachableCode();
            }
        } else if (type instanceof ReferenceType) {
            // todo: lower class types to ref types at some earlier point
            res = ptrTo(i8);
        } else {
            throw new IllegalStateException();
        }
        types.put(type, res);
        return res;
    }

}
