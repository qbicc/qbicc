package org.qbicc.plugin.llvm;

import static org.qbicc.machine.llvm.Types.array;
import static org.qbicc.machine.llvm.Types.*;
import static org.qbicc.machine.llvm.Values.*;
import static java.lang.Math.max;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.CompoundLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.ValueConvertLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.machine.llvm.Array;
import org.qbicc.machine.llvm.IdentifiedType;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Linkage;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.Struct;
import org.qbicc.machine.llvm.StructType;
import org.qbicc.machine.llvm.Types;
import org.qbicc.machine.llvm.Values;
import org.qbicc.machine.llvm.impl.LLVM;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.Type;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VariadicType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import io.smallrye.common.constraint.Assert;

final class LLVMModuleNodeVisitor implements ValueVisitor<Void, LLValue> {
    final AtomicInteger anonCnt = new AtomicInteger();
    final AtomicInteger slCnt = new AtomicInteger();
    final Module module;
    final CompilationContext ctxt;

    final Map<Type, LLValue> types = new HashMap<>();
    final Map<CompoundType, Map<CompoundType.Member, LLValue>> structureOffsets = new HashMap<>();
    final Map<Value, LLValue> globalValues = new HashMap<>();

    final Map<String, LLValue> TEMPORARY_stringLiterals = new HashMap<>();

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
            HashMap<CompoundType.Member, LLValue> offsets = new HashMap<>();
            structureOffsets.putIfAbsent(compoundType, offsets);
            String name;
            if (compoundType.getName().equals("<anon>")) {
                name = "T.anon" + anonCnt.getAndIncrement();
            } else if (compoundType.getTag() == CompoundType.Tag.NONE) {
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
                offsets.put(member, Values.intConstant(index));
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
        if (mapsToSameType(node.getValue().getType(), node.getType())) {
            return input;
        }
        LLValue fromType = map(node.getValue().getType());
        LLValue toType = map(node.getType());
        return Values.bitcastConstant(input, fromType, toType);
    }

    public LLValue visit(final Void param, final ValueConvertLiteral node) {
        LLValue input = map(node.getValue());
        ValueType inputType = node.getValue().getType();
        LLValue fromType = map(inputType);
        WordType outputType = node.getType();
        LLValue toType = map(outputType);
        if ((inputType instanceof IntegerType || inputType instanceof ReferenceType) && outputType instanceof PointerType) {
            return Values.inttoptrConstant(input, fromType, toType);
        } else if (inputType instanceof PointerType && (outputType instanceof IntegerType || outputType instanceof ReferenceType)) {
            return Values.ptrtointConstant(input, fromType, toType);
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
        int memberCnt = type.getMemberCount();
        long offs = 0;
        Struct struct = struct();
        for (int i = 0; i < memberCnt; i ++) {
            CompoundType.Member member = type.getMember(i);
            int memberOffset = member.getOffset(); // already includes alignment
            if (memberOffset > offs) {
                // we have to pad it out
                int pad = (int) (memberOffset - offs);
                struct.item(array(pad, i8), zeroinitializer);
                offs += pad;
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

    public LLValue visit(final Void param, final SymbolLiteral node) {
        return Values.global(node.getName());
    }

    public LLValue visit(final Void param, final ZeroInitializerLiteral node) {
        if (node.getType() instanceof IntegerType) {
            return Values.intConstant(0);
        } else if (node.getType() instanceof PointerType) {
            return NULL;
        } else if (node.getType() instanceof BooleanType) {
            return FALSE;
        } else if (node.getType() instanceof FloatType) {
            return Values.floatConstant(0);
        } else {
            return Values.zeroinitializer;
        }
    }

    public LLValue visit(final Void param, final BooleanLiteral node) {
        return node.booleanValue() ? TRUE : FALSE;
    }

    public LLValue visit(Void param, TypeLiteral node) {
        ObjectType type = (ObjectType)node.getValue();
        if  (type instanceof ArrayObjectType) {
            return Values.intConstant(Layout.get(ctxt).getArrayContentField(type).getEnclosingType().load().getTypeId());
        } else {
            return Values.intConstant(type.getDefinition().load().getTypeId());
        }
    }

    public LLValue visitUnknown(final Void param, final Value node) {
        ctxt.error(Location.builder().setNode(node).build(), "llvm: Unrecognized value %s", node.getClass());
        return LLVM.FALSE;
    }

    public static boolean mapsToSameType(ValueType t1, ValueType t2) {
        return t1.equals(t2)
            || t1 instanceof IntegerType && t2 instanceof IntegerType && ((IntegerType) t1).getMinBits() == ((IntegerType) t2).getMinBits()
            || t1 instanceof ReferenceType && t2 instanceof ReferenceType
            || t1 instanceof PointerType && t2 instanceof PointerType && mapsToSameType(((PointerType) t1).getPointeeType(), ((PointerType) t2).getPointeeType());
    }

    // === TEMPORARY until heap serialization is functional ===

    public LLValue visit(Void param, StringLiteral node) {
        String value = node.getValue();
        LLValue v = TEMPORARY_stringLiterals.get(value);
        if (v == null) {
            int id = slCnt.getAndIncrement();
            Layout layout = Layout.get(ctxt);
            LiteralFactory lf = ctxt.getLiteralFactory();
            TypeSystem ts = ctxt.getTypeSystem();

            DefinedTypeDefinition jlo = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object");
            Layout.LayoutInfo jloLayout = layout.getInstanceLayoutInfo(jlo);
            CompoundType.Member typeIdMem = jloLayout.getMember(layout.getObjectTypeIdField());

            DefinedTypeDefinition a = layout.getArrayLengthField().getEnclosingType();
            Layout.LayoutInfo aLayout = layout.getInstanceLayoutInfo(a);
            CompoundType.Member lengthMem = aLayout.getMember(layout.getArrayLengthField());

            DefinedTypeDefinition ba = layout.getByteArrayContentField().getEnclosingType();
            Layout.LayoutInfo baLayout = layout.getInstanceLayoutInfo(ba);
            CompoundType.Member contentMem = baLayout.getMember(layout.getByteArrayContentField());

            byte[] bytes = node.getValue().getBytes(node.isLatin1() ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_16BE);
            ArrayType s8ArrayType = ts.getArrayType(ts.getSignedInteger8Type(), bytes.length);
            CompoundType.Member realContentMem = ts.getCompoundTypeMember(contentMem.getName(), s8ArrayType, contentMem.getOffset(), contentMem.getAlign());

            CompoundType baType = ts.getCompoundType(CompoundType.Tag.NONE, "ba" + id, 0, 1, () -> List.of(typeIdMem, lengthMem, realContentMem));

            CompoundLiteral baLit = lf.literalOf(baType, Map.of(
                typeIdMem, lf.literalOfType(ba.load().getType()),
                lengthMem, lf.literalOf(bytes.length),
                realContentMem, lf.literalOf(s8ArrayType, bytes)
            ));
            module.global(map(baType)).value(map(baLit)).linkage(Linkage.PRIVATE).asGlobal("ba" + id);

            DefinedTypeDefinition jls = ctxt.getBootstrapClassContext().findDefinedType("java/lang/String");
            Layout.LayoutInfo jlsLayout = layout.getInstanceLayoutInfo(jls);
            CompoundType.Member coderMem = jlsLayout.getMember(jls.load().findField("coder"));
            CompoundType.Member valueMem = jlsLayout.getMember(jls.load().findField("value"));
            CompoundType stringType = ts.getCompoundType(CompoundType.Tag.NONE, "str" + id, 0, 1, () -> List.of(typeIdMem, coderMem, valueMem));
            CompoundLiteral lit = lf.literalOf(stringType, Map.of(
                typeIdMem, lf.literalOfType(jls.load().getType()),
                coderMem, lf.literalOf(node.isLatin1() ? 0 : 1),
                valueMem, lf.valueConvertLiteral(lf.literalOfSymbol("ba" + id, baType.getPointer()), jls.load().getType().getReference())
            ));
            module.constant(map(stringType)).value(visit(param, lit)).linkage(Linkage.PRIVATE).asGlobal("str" + id);
            TEMPORARY_stringLiterals.put(value, v = map(lf.valueConvertLiteral(lf.literalOfSymbol("str" + id, lit.getType().getPointer()), jls.load().getClassType().getReference())));
        }
        return v;
    }
}
