package org.qbicc.plugin.llvm;

import static org.qbicc.machine.llvm.Types.array;
import static org.qbicc.machine.llvm.Types.*;
import static org.qbicc.machine.llvm.Values.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.graph.InvocationNode;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.EncodeReferenceLiteral;
import org.qbicc.graph.literal.StructLiteral;
import org.qbicc.graph.literal.ElementOfLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.FunctionLiteral;
import org.qbicc.graph.literal.GlobalVariableLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralVisitor;
import org.qbicc.graph.literal.MemberOfLiteral;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.OffsetFromLiteral;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.graph.literal.ShortArrayLiteral;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.graph.literal.TypeIdLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.machine.llvm.Array;
import org.qbicc.machine.llvm.Function;
import org.qbicc.machine.llvm.FunctionAttributes;
import org.qbicc.machine.llvm.IdentifiedType;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.ParameterAttributes;
import org.qbicc.machine.llvm.Struct;
import org.qbicc.machine.llvm.Types;
import org.qbicc.machine.llvm.Values;
import org.qbicc.machine.llvm.impl.LLVM;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BlockType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.NullableType;
import org.qbicc.type.StructType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.Type;
import org.qbicc.type.UnionType;
import org.qbicc.type.UnresolvedType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VariadicType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;

final class LLVMModuleNodeVisitor implements LiteralVisitor<Void, LLValue> {
    static final LLValue i8_ptr = ptrTo(i8);
    static final LLValue i8_ptr_as1 = ptrTo(i8, 1);
    static final LLValue ptr_as1 = ptr(1);

    final LLVMModuleGenerator generator;
    final Module module;
    final CompilationContext ctxt;
    final LLVMConfiguration config;
    final boolean opaquePointers;

    final Map<Type, LLValue> types = new HashMap<>();
    final Map<StructType, Map<StructType.Member, LLValue>> structureOffsets = new HashMap<>();
    final Map<Value, LLValue> globalValues = new HashMap<>();

    final Map<FunctionType, LLValue> statepointDecls = new HashMap<>();
    final Map<String, LLValue> statepointDeclsByName = new HashMap<>();
    final Map<FunctionType, LLValue> statepointTypes = new HashMap<>();
    final Map<ValueType, LLValue> resultDecls = new HashMap<>();
    final Map<String, LLValue> resultDeclsByName = new HashMap<>();
    final Map<ValueType, LLValue> resultDeclTypes = new HashMap<>();
    final List<InvocationNode> statePointIds = new ArrayList<>();
    final LLValue refType;
    final LLValue relocateDeclType;
    LLValue relocateDecl;

    LLVMModuleNodeVisitor(final LLVMModuleGenerator generator, final Module module, final CompilationContext ctxt, final LLVMConfiguration config) {
        this.generator = generator;
        this.module = module;
        this.ctxt = ctxt;
        this.config = config;
        this.opaquePointers = config.isOpaquePointers();
        // References can be used as different types in the IL without manually casting them, so we need to
        // represent all reference types as being the same LLVM type. We will cast to and from the actual type we
        // use the reference as when needed.
        this.refType = module.identifiedType("ref").type(switch (config.getReferenceStrategy()) {
            case POINTER -> opaquePointers ? ptr : i8_ptr;
            case POINTER_AS1 -> opaquePointers ? ptr_as1 : i8_ptr_as1;
        }).asTypeRef();
        relocateDeclType = function(refType, List.of(token, i32, i32), false);
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
        } else if (type instanceof InvokableType it) {
            // LLVM does not have an equivalent to method types
            res = map(ctxt.getFunctionTypeForInvokableType(it));
        } else if (type instanceof PointerType) {
            Type pointeeType = ((PointerType) type).getPointeeType();
            res = opaquePointers ? ptr : pointeeType instanceof VoidType ? i8_ptr : ptrTo(map(pointeeType));
        } else if (type instanceof ReferenceType || type instanceof UnresolvedType) {
            res = refType;
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
        } else if (type instanceof StructType) {
            // Compound types are special in that they can be self-referential by containing pointers to themselves. To
            // handle this, we must do two special things:
            //   - Use an identified type in the module to avoid infinite recursion when printing the type
            //   - Add the mapping to types early to avoid infinite recursion when mapping self-referential member types
            StructType structType = (StructType) type;
            HashMap<StructType.Member, LLValue> offsets = new HashMap<>();
            boolean isIdentified = !structType.isAnonymous();

            structureOffsets.putIfAbsent(structType, offsets);

            IdentifiedType identifiedType = null;
            if (isIdentified) {
                String structName = structType.getName();
                String name;
                if (structType.getTag() == StructType.Tag.NONE) {
                    String outputName = "type." + structName;
                    name = LLVM.needsQuotes(structName) ? LLVM.quoteString(outputName) : outputName;
                } else {
                    String outputName = structType.getTag() + "." + structName;
                    name = LLVM.needsQuotes(structName) ? LLVM.quoteString(outputName) : outputName;
                }
                identifiedType = module.identifiedType(name);
                types.put(type, identifiedType.asTypeRef());
            }

            org.qbicc.machine.llvm.StructType struct = structType(isIdentified);
            int index = 0;
            for (StructType.Member member : structType.getPaddedMembers()) {
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
        } else if (type instanceof UnionType ut) {
            // Unions are not directly supported but pointers can be cast
            res = array((int) ut.getSize(), i8);
        } else if (type instanceof BlockType) {
            res = label;
        } else {
            throw new IllegalStateException("Can't map Type("+ type.toString() + ")");
        }
        types.put(type, res);
        return res;
    }

    LLValue map(final StructType structType, final StructType.Member member) {
        // populate map
        map(structType);
        return structureOffsets.get(structType).get(member);
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
        final ValueType inputType = node.getValue().getType();
        final WordType outputType = node.getType();
        if (config.isOpaquePointers()) {
            if (inputType instanceof PointerType && outputType instanceof PointerType || inputType instanceof ReferenceType && outputType instanceof ReferenceType) {
                return input;
            }
        }
        LLValue fromType = map(inputType);
        LLValue toType = map(outputType);
        if (fromType.equals(toType)) {
            return input;
        } else if (inputType instanceof IntegerType && outputType instanceof NullableType) {
            return Values.inttoptrConstant(input, fromType, toType);
        } else if (inputType instanceof NullableType && outputType instanceof IntegerType) {
            return Values.ptrtointConstant(input, fromType, toType);
        } else {
            return Values.bitcastConstant(input, fromType, toType);
        }
    }

    public LLValue visit(final Void param, final EncodeReferenceLiteral node) {
        LLValue input = map(node.getValue());
        PointerType inputType = node.getInputType();
        LLValue fromType = map(inputType);
        ReferenceType outputType = node.getType();
        LLValue toType = map(outputType);
        return switch (config.getReferenceStrategy()) {
            case POINTER -> input;
            case POINTER_AS1 -> Values.addrspacecastConstant(input, fromType, toType);
        };
    }

    public LLValue visit(final Void param, final ByteArrayLiteral node) {
        return Values.byteArray(node.getValues());
    }

    public LLValue visit(final Void param, final StructLiteral node) {
        StructType type = node.getType();
        Map<StructType.Member, Literal> values = node.getValues();
        // very similar to emitting a struct type, but we don't have to cache the structure offsets
        Struct struct = struct();
        for (StructType.Member member : type.getPaddedMembers()) {
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
        final Literal innermost = innermostGepValue(node);
        final PointerType ptrType = innermost.getType(PointerType.class);
        return Values.gepConstant(map(ptrType.getPointeeType()), map(ptrType), map(innermost), buildGepLiteralArgs(node, 0));
    }

    public LLValue visit(final Void param, final FloatLiteral node) {
        if (node.getType().getMinBits() == 32) {
            return Values.floatConstant(node.floatValue());
        } else { // Should be 64
            return Values.floatConstant(node.doubleValue());
        }
    }

    public LLValue visit(Void unused, FunctionLiteral literal) {
        return Values.global(literal.getExecutable().getName());
    }

    public LLValue visit(final Void unused, final GlobalVariableLiteral node) {
        return Values.global(node.getVariableElement().getName());
    }

    public LLValue visit(final Void param, final IntegerLiteral node) {
        return Values.intConstant(node.longValue());
    }

    public LLValue visit(final Void unused, final StaticFieldLiteral literal) {
        return Values.global(literal.getVariableElement().getLoweredName());
    }

    public LLValue visit(final Void unused, final MemberOfLiteral node) {
        final Literal innermost = innermostGepValue(node);
        final PointerType ptrType = innermost.getType(PointerType.class);
        return Values.gepConstant(map(ptrType.getPointeeType()), map(ptrType), map(innermost), buildGepLiteralArgs(node, 0));
    }

    public LLValue visit(final Void param, final NullLiteral node) {
        return NULL;
    }

    public LLValue visit(final Void unused, final OffsetFromLiteral node) {
        final Literal innermost = innermostGepValue(node);
        final PointerType ptrType = innermost.getType(PointerType.class);
        final LLValue mappedPointeeType;
        final LLValue mappedPtrType;
        if (ptrType.getPointeeType() instanceof InvokableType) {
            // the offset must be in bytes; change the pointer type accordingly
            final UnsignedIntegerType u8 = ptrType.getTypeSystem().getUnsignedInteger8Type();
            mappedPointeeType = map(u8);
            mappedPtrType = map(u8.getPointer());
        } else {
            mappedPointeeType = map(ptrType.getPointeeType());
            mappedPtrType = map(ptrType);
        }
        return Values.gepConstant(mappedPointeeType, mappedPtrType, map(innermost), buildGepLiteralArgs(node, 0));
    }

    public LLValue visit(final Void param, final ProgramObjectLiteral node) {
        // todo: auto-declare goes here
        return Values.global(node.getProgramObject().getName());
    }

    public LLValue visit(Void unused, ShortArrayLiteral literal) {
        return Values.array(map(literal.getType().getElementType()), literal.getValues());
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

    public LLValue visit(Void param, TypeIdLiteral node) {
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
        if (typeId <= 0) {
            ctxt.error("llvm: type %s has invalid type ID %d", type, typeId);
        }
        return Values.intConstant(typeId);
    }

    @Override
    public LLValue visitAny(Void unused, Literal literal) {
        ctxt.error(Location.builder().setNode(literal).build(), "llvm: Unrecognized literal type %s", literal.getClass());
        return LLVM.FALSE;
    }

    private LLValue[] buildGepLiteralArgs(Literal lit, int index) {
        if (lit instanceof ElementOfLiteral eol) {
            LLValue[] array = buildGepLiteralArgs(eol.getArrayPointer(), index + 2);
            final int length = array.length;
            array[length - index - 2] = map(eol.getIndex().getType());
            array[length - index - 1] = map(eol.getIndex());
            return array;
        } else if (lit instanceof MemberOfLiteral mol) {
            LLValue[] array = buildGepLiteralArgs(mol.getStructurePointer(), index + 2);
            final int length = array.length;
            array[length - index - 2] = i32;
            array[length - index - 1] = map(mol.getPointeeType(StructType.class), mol.getMember());
            return array;
        } else if (lit instanceof OffsetFromLiteral ofl) {
            // offset-from always terminates a GEP
            LLValue[] array = new LLValue[index + 2];
            // array length == index + 2; length - index = 2
            array[0] = map(ofl.getOffset().getType());
            array[1] = map(ofl.getOffset());
            return array;
        } else {
            // no other literal types can be nested GEPs, so terminate it here
            LLValue[] array = new LLValue[index + 2];
            // array length == index + 2; length - index = 2
            array[0] = i32;
            array[1] = ZERO;
            return array;
        }
    }

    private Literal innermostGepValue(Literal lit) {
        // todo: type switch
        if (lit instanceof ElementOfLiteral eol) {
            return innermostGepValue(eol.getArrayPointer());
        } else if (lit instanceof MemberOfLiteral mol) {
            return innermostGepValue(mol.getStructurePointer());
        } else if (lit instanceof OffsetFromLiteral ofl) {
            return ofl.getBasePointer();
        } else {
            return lit;
        }
    }

    public LLValue mapStatepointType(final FunctionType functionType) {
        LLValue statepointType = statepointTypes.get(functionType);
        if (statepointType == null) {
            statepointType = Types.function(token, List.of(i64, i32, map(functionType.getPointer()), i32, i32), true);
            statepointTypes.put(functionType, statepointType);
        }
        return statepointType;
    }

    public LLValue generateStatepointDecl(final FunctionType functionType) {
        LLValue statepointDecl = statepointDecls.get(functionType);
        if (statepointDecl == null) {
            StringBuilder b = new StringBuilder(64);
            b.append("llvm.experimental.gc.statepoint.");
            mapTypeSuffix(b, functionType.getPointer());
            final String name = b.toString();
            statepointDecl = statepointDeclsByName.get(name);
            if (statepointDecl == null) {
                final Function decl = module.declare(name);
                decl.param(i64).immarg(); // statepoint ID
                decl.param(i32).immarg(); // patch byte count
                // pointer to the function
                final Function.Parameter param = decl.param(map(functionType.getPointer()));
                if (generator.getLlvmMajor() >= 15) {
                    param.attribute(ParameterAttributes.elementtype(map(functionType)));
                }
                decl.param(i32).immarg(); // call arg count
                decl.param(i32).immarg(); // flags
                decl.variadic();
                decl.returns(token);
                statepointDecl = decl.asGlobal();
                statepointDeclsByName.put(name, statepointDecl);
            }
            statepointDecls.put(functionType, statepointDecl);
        }
        return statepointDecl;
    }

    private void mapTypeSuffix(final StringBuilder b, final ValueType type) {
        if (type instanceof BooleanType) {
            b.append("i1");
        } else if (type instanceof IntegerType it) {
            b.append('i').append(it.getMinBits());
        } else if (type instanceof PointerType pt) {
            b.append("p0");
            final ValueType pointeeType = pt.getPointeeType();
            if (opaquePointers && ! (pointeeType instanceof InvokableType)) {
                // nothing else to say
                return;
            }
            mapTypeSuffix(b, pointeeType);
        } else if (type instanceof ReferenceType || type instanceof UnresolvedType) {
            b.append(opaquePointers ? "p1" : "p1i8");
        } else if (type instanceof FloatType ft) {
            b.append('f').append(ft.getMinBits());
        } else if (type instanceof VoidType) {
            b.append("isVoid");
        } else if (type instanceof WordType wt) {
            b.append('i').append(wt.getMinBits());
        } else if (type instanceof StructType st) {
            b.append("s_");
            final String compoundName = st.getName();
            String name;
            if (st.getTag() == StructType.Tag.NONE) {
                String outputName = "type." + compoundName;
                name = LLVM.needsQuotes(compoundName) ? LLVM.quoteString(outputName) : outputName;
            } else {
                String outputName = st.getTag() + "." + compoundName;
                name = LLVM.needsQuotes(compoundName) ? LLVM.quoteString(outputName) : outputName;
            }
            b.append(name);
            b.append('s');
        } else if (type instanceof VariadicType) {
            // ignore
        } else if (type instanceof FunctionType ft) {
            b.append("f_");
            mapTypeSuffix(b, ft.getReturnType());
            for (ValueType parameterType : ft.getParameterTypes()) {
                mapTypeSuffix(b, parameterType);
            }
            b.append('f');
        } else {
            throw new IllegalStateException("Unexpected type " + type);
        }
    }

    public LLValue generateStatepointResultDecl(final ValueType returnType) {
        LLValue resultDecl = resultDecls.get(returnType);
        if (resultDecl == null) {
            StringBuilder b = new StringBuilder(64);
            b.append("llvm.experimental.gc.result.");
            mapTypeSuffix(b, returnType);
            final String name = b.toString();
            resultDecl = resultDeclsByName.get(name);
            if (resultDecl == null) {
                final Function decl = module.declare(b.toString());
                decl.param(token);
                decl.returns(map(returnType));
                decl.attribute(FunctionAttributes.nounwind).attribute(FunctionAttributes.readnone);
                resultDecl = decl.asGlobal();
                resultDeclsByName.put(name, resultDecl);
            }
            resultDecls.put(returnType, resultDecl);
        }
        return resultDecl;
    }

    public LLValue generateStatepointResultDeclType(final ValueType returnType) {
        LLValue resultDeclType = resultDeclTypes.get(returnType);
        if (resultDeclType == null) {
            resultDeclType = Types.function(map(returnType), List.of(token), false);
            resultDeclTypes.put(returnType, resultDeclType);
        }
        return resultDeclType;
    }

    public LLValue getRelocateDecl() {
        LLValue relocateDecl = this.relocateDecl;
        if (relocateDecl == null) {
            final Function decl = module.declare("llvm.experimental.gc.relocate");
            decl.param(token);
            decl.param(i32);
            decl.param(i32);
            decl.attribute(FunctionAttributes.nounwind);
            decl.returns(refType);
            relocateDecl = this.relocateDecl = decl.asGlobal();
        }
        return relocateDecl;
    }

    public LLValue getRelocateDeclType() {
        return relocateDeclType;
    }

    public int getNextStatePointId(final InvocationNode callNode) {
        final int id = statePointIds.size();
        statePointIds.add(callNode);
        return id;
    }

    public List<InvocationNode> getStatePointIds() {
        return statePointIds;
    }
}
