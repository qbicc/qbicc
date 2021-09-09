package org.qbicc.graph.literal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.qbicc.graph.BlockLabel;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NullableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import io.smallrye.common.constraint.Assert;

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

    FunctionParameterLiteral functionLiteralOf(String name, ValueType type);

    MethodHandleLiteral literalOfMethodHandle(int referenceKind, int referenceIndex);

    MethodDescriptorLiteral literalOfMethodDescriptor(String descriptor);

    SymbolLiteral literalOfSymbol(String name, ValueType symbolType);

    UndefinedLiteral undefinedLiteralOfType(ValueType type);

    ConstantLiteral constantLiteralOfType(ValueType type);

    TypeLiteral literalOfType(ValueType type);

    NullLiteral nullLiteralOfType(NullableType nullableType);

    Literal zeroInitializerLiteralOfType(ValueType type);

    Literal literalOf(ArrayType type, List<Literal> values);

    Literal literalOf(ArrayType type, byte[] values);

    Literal literalOf(CompoundType type, Map<CompoundType.Member, Literal> values);

    Literal bitcastLiteral(Literal value, WordType toType);

    Literal valueConvertLiteral(Literal value, WordType toType);

    Literal elementOfLiteral(Literal value, Literal index);

    static LiteralFactory create(TypeSystem typeSystem) {
        return new LiteralFactory() {
            private final BooleanLiteral TRUE = new BooleanLiteral(typeSystem.getBooleanType(), true);
            private final BooleanLiteral FALSE = new BooleanLiteral(typeSystem.getBooleanType(), false);
            private final ConcurrentMap<String, StringLiteral> stringLiterals = new ConcurrentHashMap<>();
            // todo: come up with a more efficient caching scheme
            private final ConcurrentMap<IntegerLiteral, IntegerLiteral> integerLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<FloatLiteral, FloatLiteral> floatLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, TypeLiteral> typeLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, ZeroInitializerLiteral> zeroLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<NullableType, NullLiteral> nullLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, UndefinedLiteral> undefLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, ConstantLiteral> constantLiterals = new ConcurrentHashMap<>();

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

            public MethodHandleLiteral literalOfMethodHandle(int referenceKind, int referenceIndex) {
                return new MethodHandleLiteral(typeSystem.getMethodHandleType(), referenceKind, referenceIndex);
            }

            public MethodDescriptorLiteral literalOfMethodDescriptor(String descriptor) {
                return new MethodDescriptorLiteral(typeSystem.getMethodDescriptorType(), descriptor);
            }

            public SymbolLiteral literalOfSymbol(final String name, final ValueType symbolType) {
                Assert.checkNotNullParam("name", name);
                Assert.checkNotEmptyParam("name", name);
                Assert.checkNotNullParam("symbolType", symbolType);
                return new SymbolLiteral(name, symbolType);
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

            public FunctionParameterLiteral functionLiteralOf(String name, ValueType type) {
                Assert.checkNotNullParam("name", name);
                Assert.checkNotNullParam("type", type);
                return new FunctionParameterLiteral(name, type);
            }

            public Literal literalOf(final CompoundType type, final Map<CompoundType.Member, Literal> values) {
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
            public Literal elementOfLiteral(Literal value, Literal index) {
                Assert.checkNotNullParam("value", value);
                Assert.checkNotNullParam("index", index);
                ValueType inputType = value.getType();
                if (inputType instanceof PointerType || inputType instanceof ArrayType) {
                    return value.elementOf(this, index);
                } else {
                    throw new IllegalArgumentException("Invalid input type: " + inputType);
                }
            }
        };
    }
}
