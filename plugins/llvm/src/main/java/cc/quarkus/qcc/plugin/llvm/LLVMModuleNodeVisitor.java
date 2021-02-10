package cc.quarkus.qcc.plugin.llvm;

import static cc.quarkus.qcc.machine.llvm.Types.*;
import static cc.quarkus.qcc.machine.llvm.Values.*;
import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.graph.literal.ArrayLiteral;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.CompoundLiteral;
import cc.quarkus.qcc.graph.literal.FloatLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.ZeroInitializerLiteral;
import cc.quarkus.qcc.machine.llvm.Array;
import cc.quarkus.qcc.machine.llvm.IdentifiedType;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Module;
import cc.quarkus.qcc.machine.llvm.Struct;
import cc.quarkus.qcc.machine.llvm.StructType;
import cc.quarkus.qcc.machine.llvm.Types;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.impl.LLVM;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.TypeType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.VoidType;
import cc.quarkus.qcc.type.WordType;
import io.smallrye.common.constraint.Assert;

final class LLVMModuleNodeVisitor implements ValueVisitor<Void, LLValue> {
    final Module module;
    final CompilationContext ctxt;

    final Map<Type, LLValue> types = new HashMap<>();
    final Map<CompoundType.Member, LLValue> structureOffsets = new HashMap<>();
    final Map<Value, LLValue> globalValues = new HashMap<>();

    LLVMModuleNodeVisitor(final Module module, final CompilationContext ctxt) {
        this.module = module;
        this.ctxt = ctxt;
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
        } else if (type instanceof FloatType) {
            int bytes = (int) ((FloatType) type).getSize();
            if (bytes == 4) {
                res = float32;
            } else if (bytes == 8) {
                res = float64;
            } else {
                throw Assert.unreachableCode();
            }
        } else if (type instanceof PointerType) {
            Type pointeeType = ((PointerType) type).getPointeeType();
            res = ptrTo(pointeeType instanceof VoidType ? i8 : map(pointeeType));
        } else if (type instanceof WordType) {
            // all other words are integers
            // LLVM doesn't really care about signedness
            int bytes = (int) ((WordType) type).getSize();
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
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            Type elementType = arrayType.getElementType();
            long size = arrayType.getElementCount();
            res = array((int) size, map(elementType));
        } else if (type instanceof CompoundType) {
            // Compound types are special in that they can be self-referential by containing pointers to themselves. To
            // handle this, we must do two special things:
            //   - Use an identified type in the module to avoid infinite recursion when printing the type
            //   - Add the mapping to types early to avoid infinite recursion when mapping self-referential member types
            CompoundType compoundType = (CompoundType) type;
            String name;
            if (compoundType.getTag() == CompoundType.Tag.NONE) {
                name = "T." + compoundType.getName();
            } else {
                name = "T." + compoundType.getTag() + "." + compoundType.getName();
            }
            IdentifiedType identifiedType = module.identifiedType(name);
            res = identifiedType.asTypeRef();
            types.put(type, res);

            // this is a little tricky.
            int memberCnt = compoundType.getMemberCount();
            long offs = 0;
            StructType struct = structType();
            int index = 0;
            for (int i = 0; i < memberCnt; i ++) {
                CompoundType.Member member = compoundType.getMember(i);
                int memberOffset = member.getOffset(); // already includes alignment
                if (memberOffset > offs) {
                    // we have to pad it out
                    int pad = (int) (memberOffset - offs);
                    struct.member(array(pad, i8));
                    offs += pad;
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

            identifiedType.type(struct);
        } else if (type instanceof TypeType) {
            TypeType typeType = (TypeType) type;
            int size = ctxt.getTypeSystem().getTypeIdSize();
            if (size == 1) {
                res = i8;
            } else if (size == 2) {
                res = i16;
            } else if (size == 4) {
                res = i32;
            } else {
                throw new IllegalStateException("Unsupported size for type IDs: " + size);
            }
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

    LLValue map(final Literal value) {
        LLValue mapped = globalValues.get(value);
        if (mapped != null) {
            return mapped;
        }
        mapped = value.accept(this, null);
        globalValues.put(value, mapped);
        return mapped;
    }

    public LLValue visit(final Void param, final ArrayLiteral node) {
        List<Literal> values = node.getValues();
        Array array = Values.array(map(node.getType().getElementType()));
        for (int i = 0; i < values.size(); i++) {
            array.item(map(values.get(i)));
        }
        return array;
    }

    public LLValue visit(final Void param, final CompoundLiteral node) {
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
                struct.item(map(memberType), map(literal));
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

    public LLValue visit(final Void param, final FloatLiteral node) {
        if (((FloatType) node.getType()).getMinBits() == 32) {
            return Values.floatConstant(node.floatValue());
        } else { // Should be 64
            return Values.floatConstant(node.doubleValue());
        }
    }

    public LLValue visit(final Void param, final IntegerLiteral node) {
        return Values.intConstant(node.longValue());
    }

    public LLValue visit(final Void param, final NullLiteral node) {
        return zeroinitializer;
    }

    public LLValue visit(final Void param, final SymbolLiteral node) {
        return Values.global(node.getName());
    }

    public LLValue visit(final Void param, final ZeroInitializerLiteral node) {
        return Values.zeroinitializer;
    }

    public LLValue visit(final Void param, final BooleanLiteral node) {
        return node.booleanValue() ? TRUE : FALSE;
    }

    public LLValue visitUnknown(final Void param, final Value node) {
        ctxt.error(Location.builder().setNode(node).build(), "llvm: Unrecognized value %s", node.getClass());
        return LLVM.FALSE;
    }
}
