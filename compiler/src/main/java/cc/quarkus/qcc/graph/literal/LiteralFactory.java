package cc.quarkus.qcc.graph.literal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
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

    StringLiteral literalOf(String value);

    NullLiteral literalOfNull();

    ObjectLiteral literalOf(VmObject value);

    MethodHandleLiteral literalOfMethodHandle(int referenceKind, int referenceIndex);

    MethodDescriptorLiteral literalOfMethodDescriptor(String descriptor);

    SymbolLiteral literalOfSymbol(String name, ValueType symbolType);

    CurrentThreadLiteral literalOfCurrentThread(ReferenceType threadType);

    UndefinedLiteral literalOfUndefined();

    DefinedConstantLiteral literalOfDefinedConstant(String name, Value constantValue);

    TypeLiteral literalOfType(ValueType type);

    ZeroInitializerLiteral zeroInitializerLiteralOfType(ValueType type);

    ArrayLiteral literalOf(ArrayType type, List<Literal> values);

    CompoundLiteral literalOf(CompoundType type, Map<CompoundType.Member, Literal> values);

    static LiteralFactory create(TypeSystem typeSystem) {
        return new LiteralFactory() {
            private final BooleanLiteral TRUE = new BooleanLiteral(typeSystem.getBooleanType().asConst(), true);
            private final BooleanLiteral FALSE = new BooleanLiteral(typeSystem.getBooleanType().asConst(), false);
            private final NullLiteral NULL = new NullLiteral(typeSystem.getNullType());
            private final UndefinedLiteral undef = new UndefinedLiteral(typeSystem.getPoisonType());
            private final ConcurrentMap<String, StringLiteral> stringLiterals = new ConcurrentHashMap<>();
            // todo: come up with a more efficient caching scheme
            private final ConcurrentMap<IntegerLiteral, IntegerLiteral> integerLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<FloatLiteral, FloatLiteral> floatLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, TypeLiteral> typeLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, ZeroInitializerLiteral> zeroLiterals = new ConcurrentHashMap<>();

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

            public StringLiteral literalOf(final String value) {
                return stringLiterals.computeIfAbsent(value, v -> new StringLiteral(typeSystem.getStringType(), v));
            }

            public NullLiteral literalOfNull() {
                return NULL;
            }

            public UndefinedLiteral literalOfUndefined() {
                return undef;
            }

            public DefinedConstantLiteral literalOfDefinedConstant(final String name, final Value constantValue) {
                return new DefinedConstantLiteral(name, constantValue);
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

            public CurrentThreadLiteral literalOfCurrentThread(final ReferenceType threadType) {
                return new CurrentThreadLiteral(Assert.checkNotNullParam("threadType", threadType));
            }

            public TypeLiteral literalOfType(final ValueType type) {
                Assert.checkNotNullParam("type", type);
                return typeLiterals.computeIfAbsent(type, TypeLiteral::new);
            }

            public ZeroInitializerLiteral zeroInitializerLiteralOfType(final ValueType type) {
                Assert.checkNotNullParam("type", type);
                return zeroLiterals.computeIfAbsent(type, ZeroInitializerLiteral::new);
            }

            public ArrayLiteral literalOf(final ArrayType type, final List<Literal> values) {
                Assert.checkNotNullParam("type", type);
                Assert.checkNotNullParam("values", values);
                if (type.getElementCount() != values.size()) {
                    throw new IllegalArgumentException("Cannot construct array literal with different element count than the size of the list of values");
                }
                return new ArrayLiteral(type, values);
            }

            public CompoundLiteral literalOf(final CompoundType type, final Map<CompoundType.Member, Literal> values) {
                Assert.checkNotNullParam("type", type);
                Assert.checkNotNullParam("values", values);
                return new CompoundLiteral(type, values);
            }
        };
    }
}
