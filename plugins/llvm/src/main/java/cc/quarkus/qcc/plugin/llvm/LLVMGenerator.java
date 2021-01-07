package cc.quarkus.qcc.plugin.llvm;

import static cc.quarkus.qcc.machine.llvm.Types.*;
import static cc.quarkus.qcc.machine.llvm.Values.*;
import static java.lang.Math.*;

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
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.graph.literal.ArrayLiteral;
import cc.quarkus.qcc.graph.literal.CompoundLiteral;
import cc.quarkus.qcc.graph.literal.FloatLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.ZeroInitializerLiteral;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.llvm.Array;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Linkage;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Struct;
import cc.quarkus.qcc.machine.llvm.StructType;
import cc.quarkus.qcc.machine.llvm.Types;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.impl.LLVM;
import cc.quarkus.qcc.object.Data;
import cc.quarkus.qcc.object.DataDeclaration;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.FunctionDeclaration;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.object.ProgramObject;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.VoidType;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodBody;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public class LLVMGenerator implements Consumer<CompilationContext>, ValueVisitor<CompilationContext, LLValue> {

    final Map<Type, LLValue> types = new HashMap<>();
    final Map<CompoundType.Member, LLValue> structureOffsets = new HashMap<>();
    final Map<Value, LLValue> globalValues = new HashMap<>();

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
                        for (int i = 0; i < cnt; i++) {
                            decl.param(map(fnType.getParameterType(i)));
                        }
                    } else if (item instanceof DataDeclaration) {
                        module.global(map(item.getType())).external().asGlobal(item.getName());
                    } else {
                        assert item instanceof Data;
                        module.global(map(item.getType())).value(zeroinitializer).linkage(Linkage.COMMON).asGlobal(item.getName());
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
        } else if (type instanceof PointerType) {
            Type pointeeType = ((PointerType) type).getPointeeType();
            res = ptrTo(pointeeType instanceof VoidType ? i8 : map(pointeeType));
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            Type elementType = arrayType.getElementType();
            long size = arrayType.getSize();
            res = array((int) size, map(elementType));
        } else if (type instanceof CompoundType) {
            // this is a little tricky.
            CompoundType compoundType = (CompoundType) type;
            int memberCnt = compoundType.getMemberCount();
            long offs = 0;
            StructType struct = structType();
            int index = 0;
            for (int i = 0; i < memberCnt; i ++) {
                CompoundType.Member member = compoundType.getMember(i);
                int memberOffset = member.getOffset(); // already includes alignment
                if (memberOffset > offs) {
                    // we have to pad it out
                    struct.member(array((int) (memberOffset - offs), i8));
                    index ++;
                }
                ValueType memberType = member.getType();
                struct.member(map(memberType));
                // todo: cache these ints
                structureOffsets.put(member, Values.intConstant(index));
                index ++;
                // the target will already pad out for normal alignment
                offs += max(memberType.getAlign(), memberType.getSize());
            }
            long size = compoundType.getSize();
            if (offs < size) {
                // yet more padding
                struct.member(array((int) (size - offs), i8));
            }
            res = struct;
        } else {
            throw new IllegalStateException();
        }
        types.put(type, res);
        return res;
    }

    LLValue map(final CompoundType compoundType, final CompoundType.Member member) {
        // populate map
        map(compoundType);
        return structureOffsets.get(member);
    }

    LLValue map(final CompilationContext ctxt, final Literal value) {
        LLValue mapped = globalValues.get(value);
        if (mapped != null) {
            return mapped;
        }
        mapped = value.accept(this, ctxt);
        globalValues.put(value, mapped);
        return mapped;
    }

    public LLValue visit(final CompilationContext param, final ArrayLiteral node) {
        List<Literal> values = node.getValues();
        Array array = Values.array(map(node.getType().getElementType()));
        for (int i = 0; i < values.size(); i++) {
            array.item(map(param, values.get(i)));
        }
        return array;
    }

    public LLValue visit(final CompilationContext param, final CompoundLiteral node) {
        CompoundType type = node.getType();
        Map<CompoundType.Member, Literal> values = node.getValues();
        // very similar to emitting a struct type, but we don't have to cache the structure offsets
        int memberCnt = type.getMemberCount();
        long offs = 0;
        Struct struct = struct();
        for (int i = 0; i < memberCnt; i ++) {
            CompoundType.Member member = type.getMember(i);
            int memberOffset = member.getOffset(); // already includes alignment
            if (memberOffset > offs) {
                // we have to pad it out
                struct.item(array((int) (memberOffset - offs), i8), zeroinitializer);
            }
            // actual member
            Literal literal = values.get(member);
            ValueType memberType = member.getType();
            if (literal == null) {
                // no value for this member
                struct.item(map(memberType), zeroinitializer);
            } else {
                struct.item(map(memberType), map(param, literal));
            }
            // the target will already pad out for normal alignment
            offs += max(memberType.getAlign(), memberType.getSize());
        }
        long size = type.getSize();
        if (offs < size) {
            // yet more padding
            struct.item(array((int) (size - offs), i8), zeroinitializer);
        }
        return struct;
    }

    public LLValue visit(final CompilationContext param, final FloatLiteral node) {
        if (((FloatType) node.getType()).getMinBits() == 32) {
            return Values.floatConstant(node.floatValue());
        } else { // Should be 64
            return Values.floatConstant(node.doubleValue());
        }
    }

    public LLValue visit(final CompilationContext param, final IntegerLiteral node) {
        return Values.intConstant(node.longValue());
    }

    public LLValue visit(final CompilationContext param, final NullLiteral node) {
        return NULL;
    }

    public LLValue visit(final CompilationContext param, final SymbolLiteral node) {
        return Values.global(node.getName());
    }

    public LLValue visit(final CompilationContext param, final ZeroInitializerLiteral node) {
        return Values.zeroinitializer;
    }

    public LLValue visitUnknown(final CompilationContext ctxt, final Value node) {
        ctxt.error(Location.builder().setNode(node).build(), "llvm: Unrecognized value %s", node.getClass());
        return LLVM.FALSE;
    }
}
