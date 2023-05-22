package org.qbicc.graph.literal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import io.smallrye.common.constraint.Assert;
import org.qbicc.graph.BlockLabel;
import org.qbicc.interpreter.VmObject;
import org.qbicc.object.ProgramObject;
import org.qbicc.pointer.ConstructorPointer;
import org.qbicc.pointer.ElementPointer;
import org.qbicc.pointer.FunctionPointer;
import org.qbicc.pointer.GlobalPointer;
import org.qbicc.pointer.InitializerPointer;
import org.qbicc.pointer.InstanceMethodPointer;
import org.qbicc.pointer.IntegerAsPointer;
import org.qbicc.pointer.MemberPointer;
import org.qbicc.pointer.OffsetPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.ProgramObjectPointer;
import org.qbicc.pointer.StaticFieldPointer;
import org.qbicc.pointer.StaticMethodPointer;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.StructType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NullableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticMethodElement;
import org.qbicc.type.definition.element.VariableElement;
import org.qbicc.type.methodhandle.MethodHandleConstant;

/**
 *
 */
public interface LiteralFactory {
    BlockLiteral literalOf(BlockLabel blockLabel);

    BooleanLiteral literalOf(boolean value);

    FloatLiteral literalOf(float value);

    FloatLiteral literalOf(double value);

    FloatLiteral literalOf(FloatType type, double value);

    IntegerLiteral literalOf(long value);
    IntegerLiteral literalOf(int value);
    IntegerLiteral literalOf(short value);
    IntegerLiteral literalOf(byte value);

    IntegerLiteral literalOf(char value);

    IntegerLiteral literalOf(IntegerType type, long value);

    StringLiteral literalOf(String value, ReferenceType stringRefType);

    ObjectLiteral literalOf(VmObject value);

    MethodHandleLiteral literalOfMethodHandle(MethodHandleConstant methodHandleConstant, ReferenceType type);

    ProgramObjectLiteral literalOf(ProgramObject programObject);

    UndefinedLiteral undefinedLiteralOfType(ValueType type);

    ConstantLiteral constantLiteralOfType(ValueType type);

    TypeLiteral literalOfType(ValueType type);

    NullLiteral nullLiteralOfType(NullableType nullableType);

    Literal zeroInitializerLiteralOfType(ValueType type);

    Literal literalOf(ArrayType type, List<Literal> values);

    Literal literalOf(ArrayType type, byte[] values);

    Literal literalOf(ArrayType type, short[] values);

    Literal literalOf(StructType type, Map<StructType.Member, Literal> values);

    Literal bitcastLiteral(Literal value, WordType toType);

    Literal valueConvertLiteral(Literal value, WordType toType);

    ElementOfLiteral elementOfLiteral(Literal arrayPointer, Literal index);

    MemberOfLiteral memberOfLiteral(Literal structurePointer, StructType.Member member);

    OffsetFromLiteral offsetFromLiteral(Literal basePointer, Literal offset);

    default Literal literalOf(Pointer value) {
        return value.accept(new Pointer.Visitor<LiteralFactory, Literal>() {
            @Override
            public Literal visitAny(LiteralFactory literalFactory, Pointer pointer) {
                throw new IllegalArgumentException("Invalid pointer");
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, ConstructorPointer pointer) {
                return literalFactory.literalOf(pointer.getExecutableElement());
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, FunctionPointer pointer) {
                return literalFactory.literalOf(pointer.getExecutableElement());
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, GlobalPointer pointer) {
                return literalFactory.literalOf(pointer.getGlobalVariable());
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, InitializerPointer pointer) {
                return literalFactory.literalOf(pointer.getExecutableElement());
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, InstanceMethodPointer pointer) {
                return literalFactory.literalOf(pointer.getExecutableElement());
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, IntegerAsPointer pointer) {
                return literalFactory.bitcastLiteral(literalFactory.literalOf(pointer.getType().getSameSizedUnsignedInteger(), pointer.getValue()), pointer.getType());
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, ProgramObjectPointer pointer) {
                return literalFactory.literalOf(pointer.getProgramObject());
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, StaticFieldPointer pointer) {
                return literalFactory.literalOf(pointer.getStaticField());
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, StaticMethodPointer pointer) {
                return literalFactory.literalOf(pointer.getExecutableElement());
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, ElementPointer pointer) {
                return literalFactory.elementOfLiteral(literalFactory.literalOf(pointer.getArrayPointer()), literalFactory.literalOf(pointer.getIndex()));
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, MemberPointer pointer) {
                return literalFactory.memberOfLiteral(literalFactory.literalOf(pointer.getStructurePointer()), pointer.getMember());
            }

            @Override
            public Literal visit(LiteralFactory literalFactory, OffsetPointer pointer) {
                return literalFactory.offsetFromLiteral(literalFactory.literalOf(pointer.getBasePointer()), literalFactory.literalOf(pointer.getOffset()));
            }
        }, this);
    }

    GlobalVariableLiteral literalOf(GlobalVariableElement variableElement);

    StaticFieldLiteral literalOf(StaticFieldElement variableElement);

    AsmLiteral literalOfAsm(String instructions, String constraints, FunctionType type, AsmLiteral.Flag... flags);

    default ExecutableLiteral literalOf(ExecutableElement element) {
        if (element instanceof InitializerElement ie) {
            return literalOf(ie);
        } else if (element instanceof InvokableElement ie) {
            return literalOf(ie);
        } else {
            throw new IllegalStateException();
        }
    }

    default InvokableLiteral literalOf(InvokableElement element) {
        if (element instanceof ConstructorElement ce) {
            return literalOf(ce);
        } else if (element instanceof FunctionElement fe) {
            return literalOf(fe);
        } else if (element instanceof MethodElement me) {
            return literalOf(me);
        } else {
            throw new IllegalStateException();
        }
    }

    default MethodLiteral literalOf(MethodElement element) {
        if (element instanceof InstanceMethodElement ime) {
            return literalOf(ime);
        } else if (element instanceof StaticMethodElement sme) {
            return literalOf(sme);
        } else {
            throw new IllegalStateException();
        }
    }

    StaticMethodLiteral literalOf(StaticMethodElement element);

    ConstructorLiteral literalOf(ConstructorElement element);

    InstanceMethodLiteral literalOf(InstanceMethodElement element);

    InitializerLiteral literalOf(InitializerElement element);

    FunctionLiteral literalOf(FunctionElement element);

    static LiteralFactory create(TypeSystem typeSystem) {
        return new LiteralFactory() {
            private final BooleanLiteral TRUE = new BooleanLiteral(typeSystem.getBooleanType(), true);
            private final BooleanLiteral FALSE = new BooleanLiteral(typeSystem.getBooleanType(), false);
            private final ConcurrentMap<String, StringLiteral> stringLiterals = new ConcurrentHashMap<>();
            // todo: come up with a more efficient caching scheme
            private final ConcurrentMap<IntegerLiteral, IntegerLiteral> integerLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<FloatLiteral, FloatLiteral> floatLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ProgramObject, ProgramObjectLiteral> programObjects = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, TypeLiteral> typeLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, ZeroInitializerLiteral> zeroLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<NullableType, NullLiteral> nullLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, UndefinedLiteral> undefLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, ConstantLiteral> constantLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<VariableElement, VariableLiteral> varLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ExecutableElement, ExecutableLiteral> exeLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<Literal, ConcurrentMap<Literal, ElementOfLiteral>> elementLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<Literal, ConcurrentMap<StructType.Member, MemberOfLiteral>> memberLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<Literal, ConcurrentMap<Literal, OffsetFromLiteral>> offsetLiterals = new ConcurrentHashMap<>();

            public BlockLiteral literalOf(final BlockLabel blockLabel) {
                return new BlockLiteral(typeSystem.getBlockType(), blockLabel);
            }

            public BooleanLiteral literalOf(final boolean value) {
                return value ? TRUE : FALSE;
            }

            public FloatLiteral literalOf(final float value) {
                return literalOf(typeSystem.getFloat32Type(), value);
            }

            public FloatLiteral literalOf(final double value) {
                return literalOf(typeSystem.getFloat64Type(), value);
            }

            public FloatLiteral literalOf(FloatType type, final double value) {
                FloatLiteral v = new FloatLiteral(type, value);
                return floatLiterals.computeIfAbsent(v, Function.identity());
            }

            public IntegerLiteral literalOf(final long value) {
                return literalOf(typeSystem.getSignedInteger64Type(), value);
            }

            public IntegerLiteral literalOf(final int value) {
                return literalOf(typeSystem.getSignedInteger32Type(), value);
            }

            public IntegerLiteral literalOf(final short value) {
                return literalOf(typeSystem.getSignedInteger16Type(), value);
            }

            public IntegerLiteral literalOf(final byte value) {
                return literalOf(typeSystem.getSignedInteger8Type(), value);
            }

            public IntegerLiteral literalOf(final char value) {
                return literalOf(typeSystem.getUnsignedInteger16Type(), value);
            }

            public IntegerLiteral literalOf(IntegerType type, final long value) {
                IntegerLiteral v = new IntegerLiteral(type, value);
                return integerLiterals.computeIfAbsent(v, Function.identity());
            }

            public StringLiteral literalOf(final String value, ReferenceType stringRefType) {
                return stringLiterals.computeIfAbsent(value, v -> new StringLiteral(stringRefType, v));
            }

            public UndefinedLiteral undefinedLiteralOfType(ValueType type) {
                return undefLiterals.computeIfAbsent(type, UndefinedLiteral::new);
            }

            public ConstantLiteral constantLiteralOfType(ValueType type) {
                return constantLiterals.computeIfAbsent(type, ConstantLiteral::new);
            }

            public ObjectLiteral literalOf(final VmObject value) {
                Assert.checkNotNullParam("value", value);
                // todo: cache on object itself?
                return new ObjectLiteral(value.getObjectType().getReference(), value);
            }

            public MethodHandleLiteral literalOfMethodHandle(MethodHandleConstant methodHandleConstant, ReferenceType type) {
                return new MethodHandleLiteral(methodHandleConstant, type);
            }

            public ProgramObjectLiteral literalOf(ProgramObject programObject) {
                return programObjects.computeIfAbsent(programObject, ProgramObjectLiteral::new);
            }

            public TypeLiteral literalOfType(final ValueType type) {
                Assert.checkNotNullParam("type", type);
                return typeLiterals.computeIfAbsent(type, TypeLiteral::new);
            }

            public NullLiteral nullLiteralOfType(NullableType type) {
                Assert.checkNotNullParam("type", type);
                return nullLiterals.computeIfAbsent(type, NullLiteral::new);
            }

            public Literal zeroInitializerLiteralOfType(final ValueType type) {
                Assert.checkNotNullParam("type", type);
                if (type instanceof IntegerType) {
                    return literalOf((IntegerType) type, 0);
                } else if (type instanceof FloatType) {
                    return literalOf((FloatType) type, 0.0);
                } else if (type instanceof BooleanType) {
                    return literalOf(false);
                } else if (type instanceof NullableType) {
                    return nullLiteralOfType((NullableType) type);
                }
                return zeroLiterals.computeIfAbsent(type, ZeroInitializerLiteral::new);
            }

            public Literal literalOf(final ArrayType type, final List<Literal> values) {
                Assert.checkNotNullParam("type", type);
                Assert.checkNotNullParam("values", values);
                if (type.getElementCount() != values.size()) {
                    throw new IllegalArgumentException("Cannot construct array literal with different element count than the size of the list of values");
                }
                for (Literal value : values) {
                    if (value.isNonZero()) {
                        return new ArrayLiteral(type, values);
                    }
                }
                return zeroInitializerLiteralOfType(type);
            }

            public Literal literalOf(ArrayType type, byte[] values) {
                Assert.checkNotNullParam("type", type);
                Assert.checkNotNullParam("values", values);
                if (type.getElementCount() != values.length) {
                    throw new IllegalArgumentException("Cannot construct array literal with different element count than the size of the list of values");
                }
                for (byte value : values) {
                    if (value != 0) {
                        return new ByteArrayLiteral(type, values);
                    }
                }
                return zeroInitializerLiteralOfType(type);
            }

            public Literal literalOf(ArrayType type, short[] values) {
                Assert.checkNotNullParam("type", type);
                Assert.checkNotNullParam("values", values);
                if (type.getElementCount() != values.length) {
                    throw new IllegalArgumentException("Cannot construct array literal with different element count than the size of the list of values");
                }
                for (short value : values) {
                    if (value != 0) {
                        return new ShortArrayLiteral(type, values);
                    }
                }
                return zeroInitializerLiteralOfType(type);
            }

            public Literal literalOf(final StructType type, final Map<StructType.Member, Literal> values) {
                Assert.checkNotNullParam("type", type);
                Assert.checkNotNullParam("values", values);
                for (Literal value : values.values()) {
                    if (value.isNonZero()) {
                        return new CompoundLiteral(type, values);
                    }
                }
                return zeroInitializerLiteralOfType(type);
            }

            public Literal bitcastLiteral(final Literal value, final WordType toType) {
                Assert.checkNotNullParam("value", value);
                Assert.checkNotNullParam("toType", toType);
                return value.bitCast(this, toType);
            }

            public Literal valueConvertLiteral(final Literal value, final WordType toType) {
                Assert.checkNotNullParam("value", value);
                Assert.checkNotNullParam("toType", toType);
                return value.convert(this, toType);
            }

            @Override
            public ElementOfLiteral elementOfLiteral(Literal arrayPointer, Literal index) {
                Assert.checkNotNullParam("arrayPointer", arrayPointer);
                Assert.checkNotNullParam("index", index);
                ValueType pointeeType = arrayPointer.getPointeeType();
                if (!(pointeeType instanceof ArrayType)) {
                    throw new IllegalArgumentException("Array pointer is of wrong type " + pointeeType);
                }
                final ValueType indexType = index.getType();
                if (! (indexType instanceof IntegerType)) {
                    throw new IllegalArgumentException("Index is of wrong type " + indexType);
                }
                final ConcurrentMap<Literal, ElementOfLiteral> subMap = elementLiterals.computeIfAbsent(arrayPointer, LiteralFactory::newMap);
                ElementOfLiteral lit = subMap.get(index);
                if (lit == null) {
                    lit = new ElementOfLiteral(arrayPointer, index);
                    final ElementOfLiteral appearing = subMap.putIfAbsent(index, lit);
                    if (appearing != null) {
                        lit = appearing;
                    }
                }
                return lit;
            }

            @Override
            public MemberOfLiteral memberOfLiteral(Literal structurePointer, StructType.Member member) {
                Assert.checkNotNullParam("structurePointer", structurePointer);
                Assert.checkNotNullParam("member", member);
                ValueType structureType = structurePointer.getPointeeType();
                if (! (structureType instanceof StructType)) {
                    throw new IllegalArgumentException("Structure pointer is of wrong type " + structureType);
                }
                final ConcurrentMap<StructType.Member, MemberOfLiteral> subMap = memberLiterals.computeIfAbsent(structurePointer, LiteralFactory::newMap);
                MemberOfLiteral lit = subMap.get(member);
                if (lit == null) {
                    lit = new MemberOfLiteral(structurePointer, member);
                    final MemberOfLiteral appearing = subMap.putIfAbsent(member, lit);
                    if (appearing != null) {
                        lit = appearing;
                    }
                }
                return lit;
            }

            @Override
            public OffsetFromLiteral offsetFromLiteral(Literal basePointer, Literal offset) {
                Assert.checkNotNullParam("basePointer", basePointer);
                Assert.checkNotNullParam("offset", offset);
                final ValueType basePointerType = basePointer.getType();
                if (! (basePointerType instanceof PointerType)) {
                    throw new IllegalArgumentException("Base pointer is of wrong type " + basePointerType);
                }
                final ValueType offsetType = offset.getType();
                if (! (offsetType instanceof IntegerType)) {
                    throw new IllegalArgumentException("Offset is of wrong type " + offsetType);
                }
                final ConcurrentMap<Literal, OffsetFromLiteral> subMap = offsetLiterals.computeIfAbsent(basePointer, LiteralFactory::newMap);
                OffsetFromLiteral lit = subMap.get(offset);
                if (lit == null) {
                    lit = new OffsetFromLiteral(basePointer, offset);
                    final OffsetFromLiteral appearing = subMap.putIfAbsent(offset, lit);
                    if (appearing != null) {
                        lit = appearing;
                    }
                }
                return lit;
            }

            @Override
            public GlobalVariableLiteral literalOf(GlobalVariableElement variableElement) {
                Assert.checkNotNullParam("variableElement", variableElement);
                return (GlobalVariableLiteral) varLiterals.computeIfAbsent(variableElement, GlobalVariableLiteral::new);
            }

            @Override
            public StaticFieldLiteral literalOf(StaticFieldElement variableElement) {
                Assert.checkNotNullParam("variableElement", variableElement);
                return (StaticFieldLiteral) varLiterals.computeIfAbsent(variableElement, StaticFieldLiteral::new);
            }

            @Override
            public AsmLiteral literalOfAsm(String instructions, String constraints, FunctionType type, AsmLiteral.Flag... flags) {
                Assert.checkNotNullParam("instructions", instructions);
                Assert.checkNotNullParam("constraints", constraints);
                Assert.checkNotNullParam("type", type);
                Set<AsmLiteral.Flag> flagSet = flags == null ? Set.of() : Set.of(flags);
                return new AsmLiteral(instructions, constraints, flagSet, type);
            }

            @Override
            public StaticMethodLiteral literalOf(StaticMethodElement element) {
                Assert.checkNotNullParam("element", element);
                return (StaticMethodLiteral) exeLiterals.computeIfAbsent(element, StaticMethodLiteral::new);
            }

            @Override
            public ConstructorLiteral literalOf(ConstructorElement element) {
                Assert.checkNotNullParam("element", element);
                return (ConstructorLiteral) exeLiterals.computeIfAbsent(element, ConstructorLiteral::new);
            }

            @Override
            public InstanceMethodLiteral literalOf(InstanceMethodElement element) {
                Assert.checkNotNullParam("element", element);
                return (InstanceMethodLiteral) exeLiterals.computeIfAbsent(element, InstanceMethodLiteral::new);
            }

            @Override
            public InitializerLiteral literalOf(InitializerElement element) {
                Assert.checkNotNullParam("element", element);
                return (InitializerLiteral) exeLiterals.computeIfAbsent(element, InitializerLiteral::new);
            }

            @Override
            public FunctionLiteral literalOf(FunctionElement element) {
                Assert.checkNotNullParam("element", element);
                return (FunctionLiteral) exeLiterals.computeIfAbsent(element, FunctionLiteral::new);
            }
        };
    }

    private static <K, V> ConcurrentMap<K, V> newMap(Object ignored) {
        return new ConcurrentHashMap<>();
    }
}
