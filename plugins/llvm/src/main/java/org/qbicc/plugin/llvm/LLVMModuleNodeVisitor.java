package org.qbicc.plugin.llvm;

import static org.qbicc.machine.llvm.Types.array;
import static org.qbicc.machine.llvm.Types.*;
import static org.qbicc.machine.llvm.Values.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.CompoundLiteral;
import org.qbicc.graph.literal.ElementOfLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.graph.literal.ValueConvertLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.machine.llvm.Array;
import org.qbicc.machine.llvm.IdentifiedType;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.Struct;
import org.qbicc.machine.llvm.StructType;
import org.qbicc.machine.llvm.Types;
import org.qbicc.machine.llvm.Values;
import org.qbicc.machine.llvm.impl.LLVM;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.pointer.ElementPointer;
import org.qbicc.pointer.IntegerAsPointer;
import org.qbicc.pointer.MemberPointer;
import org.qbicc.pointer.OffsetPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.MethodType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.Type;
import org.qbicc.type.UnresolvedType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VariadicType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;

final class LLVMModuleNodeVisitor implements ValueVisitor<Void, LLValue>, Pointer.Visitor<PointerLiteral, LLValue> {
    final AtomicInteger anonCnt = new AtomicInteger();
    final Module module;
    final CompilationContext ctxt;
    private LLVMReferencePointerFactory refFactory;

    final Map<Type, LLValue> types = new HashMap<>();
    final Map<CompoundType, Map<CompoundType.Member, LLValue>> structureOffsets = new HashMap<>();
    final Map<Value, LLValue> globalValues = new HashMap<>();

    LLVMModuleNodeVisitor(final Module module, final CompilationContext ctxt, LLVMReferencePointerFactory refFactory) {
        this.module = module;
        this.ctxt = ctxt;
        this.refFactory = refFactory;
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
            boolean variadic = false;
            for (int i = 0; i < cnt; i ++) {
                ValueType parameterType = fnType.getParameterType(i);
                if (parameterType instanceof VariadicType) {
                    if (i < cnt - 1) {
                        throw new IllegalStateException("Variadic type as non-final parameter type");
                    }
                    variadic = true;
                } else {
                    argTypes.add(map(parameterType));
                }
            }
            res = Types.function(map(fnType.getReturnType()), argTypes, variadic);
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
        } else if (type instanceof MethodType) {
            // LLVM does not have an equivalent to method types
            res = i8;
        } else if (type instanceof PointerType) {
            Type pointeeType = ((PointerType) type).getPointeeType();
            res = ptrTo(pointeeType instanceof VoidType ? i8 : map(pointeeType), 0);
        } else if (type instanceof ReferenceType || type instanceof UnresolvedType) {
            // References can be used as different types in the IL without manually casting them, so we need to
            // represent all reference types as being the same LLVM type. We will cast to and from the actual type we
            // use the reference as when needed.
            res = refFactory.makeReferencePointer();
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
            HashMap<CompoundType.Member, LLValue> offsets = new HashMap<>();
            boolean isIdentified = !compoundType.isAnonymous();

            structureOffsets.putIfAbsent(compoundType, offsets);

            IdentifiedType identifiedType = null;
            if (isIdentified) {
                String compoundName = compoundType.getName();
                String name;
                if (compoundType.getTag() == CompoundType.Tag.NONE) {
                    String outputName = "type." + compoundName;
                    name = LLVM.needsQuotes(compoundName) ? LLVM.quoteString(outputName) : outputName;
                } else {
                    String outputName = compoundType.getTag() + "." + compoundName;
                    name = LLVM.needsQuotes(compoundName) ? LLVM.quoteString(outputName) : outputName;
                }
                identifiedType = module.identifiedType(name);
                types.put(type, identifiedType.asTypeRef());
            }

            StructType struct = structType(isIdentified);
            int index = 0;
            for (CompoundType.Member member : compoundType.getPaddedMembers()) {
                ValueType memberType = member.getType();
                struct.member(map(memberType), member.getName());
                // todo: cache these ints
                offsets.put(member, Values.intConstant(index));
                index ++;
                // the target will already pad out for normal alignment
            }

            if (isIdentified) {
                identifiedType.type(struct);
                res = identifiedType.asTypeRef();
            } else {
                res = struct;
            }
        } else {
            throw new IllegalStateException("Can't map Type("+ type.toString() + ")");
        }
        types.put(type, res);
        return res;
    }

    LLValue map(final CompoundType compoundType, final CompoundType.Member member) {
        // populate map
        map(compoundType);
        return structureOffsets.get(compoundType).get(member);
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

    public LLValue visit(final Void param, final BitCastLiteral node) {
        LLValue input = map(node.getValue());
        LLValue fromType = map(node.getValue().getType());
        LLValue toType = map(node.getType());
        if (fromType.equals(toType)) {
            return input;
        }

        return Values.bitcastConstant(input, fromType, toType);
    }

    public LLValue visit(final Void param, final ValueConvertLiteral node) {
        LLValue input = map(node.getValue());
        ValueType inputType = node.getValue().getType();
        LLValue fromType = map(inputType);
        WordType outputType = node.getType();
        LLValue toType = map(outputType);
        if (fromType.equals(toType)) {
            return input;
        }

        if (inputType instanceof IntegerType && outputType instanceof PointerType) {
            return Values.inttoptrConstant(input, fromType, toType);
        } else if (inputType instanceof PointerType && outputType instanceof IntegerType) {
            return Values.ptrtointConstant(input, fromType, toType);
        } else if (inputType instanceof ReferenceType && outputType instanceof PointerType) {
            return refFactory.cast(input, fromType, toType);
        } else if (inputType instanceof PointerType && outputType instanceof ReferenceType) {
            return refFactory.cast(input, fromType, toType);
        }
        // todo: add signed/unsigned int <-> fp
        return visitUnknown(param, node);
    }

    public LLValue visit(final Void param, final ByteArrayLiteral node) {
        return Values.byteArray(node.getValues());
    }

    public LLValue visit(final Void param, final CompoundLiteral node) {
        CompoundType type = node.getType();
        Map<CompoundType.Member, Literal> values = node.getValues();
        // very similar to emitting a struct type, but we don't have to cache the structure offsets
        Struct struct = struct();
        for (CompoundType.Member member : type.getPaddedMembers()) {
            Literal literal = values.get(member);
            ValueType memberType = member.getType();
            if (literal == null) {
                // no value for this member
                struct.item(map(memberType), zeroinitializer);
            } else {
                struct.item(map(memberType), map(literal));
            }
        }
        return struct;
    }

    public LLValue visit(final Void param, final ElementOfLiteral node) {
        PointerType pointerType = (PointerType) node.getType();
        return Values.gepConstant(map(pointerType.getPointeeType()), map(pointerType), map(node.getValue()), map(node.getIndex().getType()), map(node.getIndex()));
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
        return NULL;
    }

    public LLValue visit(final Void param, final PointerLiteral node) {
        // see below for pointer visitor implementations
        return node.getPointer().accept(this, node);
    }

    public LLValue visit(final Void param, final ZeroInitializerLiteral node) {
        return Values.zeroinitializer;
    }

    public LLValue visit(final Void param, final BooleanLiteral node) {
        return node.booleanValue() ? TRUE : FALSE;
    }

    public LLValue visit(Void param, UndefinedLiteral node) {
        return Values.UNDEF;
    }

    public LLValue visit(Void param, TypeLiteral node) {
        ValueType type = node.getValue();
        // common cases first
        int typeId;
        if (type instanceof ArrayObjectType) {
            typeId = CoreClasses.get(ctxt).getArrayContentField((ArrayObjectType) type).getEnclosingType().load().getTypeId();
        } else if (type instanceof ObjectType) {
            typeId = ((ObjectType) type).getDefinition().load().getTypeId();
        } else if (type instanceof WordType) {
            typeId = ((WordType) type).asPrimitive().getTypeId();
        } else if (type instanceof VoidType) {
            typeId = ((VoidType) type).asPrimitive().getTypeId();
        } else {
            // not a valid type literal
            ctxt.error("llvm: cannot lower type literal %s", node);
            return Values.intConstant(0);
        }
        if (typeId == 0) {
            ctxt.error("llvm: type %s does not have a valid type ID", type);
        }
        return Values.intConstant(typeId);
    }

    public LLValue visitUnknown(final Void param, final Value node) {
        ctxt.error(Location.builder().setNode(node).build(), "llvm: Unrecognized value %s", node.getClass());
        return LLVM.FALSE;
    }

    @Override
    public LLValue visitAny(PointerLiteral pointerLiteral, Pointer pointer) {
        ctxt.error(Location.builder().setNode(pointerLiteral).build(), "llvm: Unrecognized pointer value %s", pointer.getClass());
        return LLVM.FALSE;
    }

    @Override
    public LLValue visit(PointerLiteral pointerLiteral, ElementPointer pointer) {
        // todo: we can merge GEPs
        return Values.gepConstant(
            map(pointer.getPointeeType()),
            map(pointer.getType()),
            pointer.getArrayPointer().accept(this, pointerLiteral),
            ZERO,
            Values.intConstant(pointer.getIndex())
        );
    }

    @Override
    public LLValue visit(PointerLiteral pointerLiteral, MemberPointer pointer) {
        // todo: we can merge GEPs
        return Values.gepConstant(
            map(pointer.getPointeeType()),
            map(pointer.getType()),
            pointer.getStructurePointer().accept(this, pointerLiteral),
            ZERO,
            map(pointer.getPointeeType(), pointer.getMember())
        );
    }

    @Override
    public LLValue visit(PointerLiteral pointerLiteral, OffsetPointer pointer) {
        return Values.gepConstant(
            map(pointer.getPointeeType()),
            map(pointer.getType()),
            pointer.getBasePointer().accept(this, pointerLiteral),
            Values.intConstant(pointer.getOffset())
        );
    }

    @Override
    public LLValue visit(PointerLiteral pointerLiteral, IntegerAsPointer pointer) {
        return Values.inttoptrConstant(Values.intConstant(pointer.getValue()), i64, map(pointer.getType()));
    }

    @Override
    public LLValue visit(PointerLiteral pointerLiteral, ProgramObjectPointer pointer) {
        return Values.global(pointer.getProgramObject().getName());
    }
}
