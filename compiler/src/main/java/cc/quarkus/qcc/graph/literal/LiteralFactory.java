package cc.quarkus.qcc.graph.literal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.interpreter.VmObject;
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

    IntegerLiteral literalOf(long value);
    IntegerLiteral literalOf(int value);
    IntegerLiteral literalOf(short value);
    IntegerLiteral literalOf(byte value);

    IntegerLiteral literalOf(char value);

    StringLiteral literalOf(String value);

    NullLiteral literalOfNull();

    ObjectLiteral literalOf(VmObject value);

    MethodHandleLiteral literalOfMethodHandle(int referenceKind, int referenceIndex);

    MethodDescriptorLiteral literalOfMethodDescriptor(String descriptor);

    ValueArrayTypeIdLiteral literalOfArrayType(ValueType elementType);

    ReferenceArrayTypeIdLiteral literalOfArrayType(ReferenceType elementType);

    ClassTypeIdLiteral baseClassLiteral();

    ClassTypeIdLiteral literalOfClass(String className, ClassTypeIdLiteral superClass, InterfaceTypeIdLiteral... interfaces);

    InterfaceTypeIdLiteral literalOfInterface(String interfaceName, InterfaceTypeIdLiteral... interfaces);

    SymbolLiteral literalOfSymbol(String name, ValueType symbolType);

    CurrentThreadLiteral literalOfCurrentThread(ReferenceType threadType);

    UndefinedLiteral literalOfUndefined();

    DefinedConstantLiteral literalOfDefinedConstant(Value constantValue);

    TypeLiteral literalOfType(ValueType type);

    static LiteralFactory create(TypeSystem typeSystem) {
        return new LiteralFactory() {
            private final BooleanLiteral TRUE = new BooleanLiteral(typeSystem.getBooleanType().asConst(), true);
            private final BooleanLiteral FALSE = new BooleanLiteral(typeSystem.getBooleanType().asConst(), false);
            private final NullLiteral NULL = new NullLiteral(typeSystem.getNullType());
            private final UndefinedLiteral undef = new UndefinedLiteral(typeSystem.getPoisonType());
            private final ClassTypeIdLiteral baseClassLiteral = new ClassTypeIdLiteral("java/lang/Object", null, InterfaceTypeIdLiteral.NONE, typeSystem.getTypeIdType());
            private final ConcurrentMap<ValueType, ValueArrayTypeIdLiteral> primitiveArrayLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<TypeIdLiteral, ReferenceArrayTypeIdLiteral> arrayObjectTypeIdLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<String, StringLiteral> stringLiterals = new ConcurrentHashMap<>();
            // todo: come up with a more efficient caching scheme
            private final ConcurrentMap<IntegerLiteral, IntegerLiteral> integerLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<FloatLiteral, FloatLiteral> floatLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<ValueType, TypeLiteral> typeLiterals = new ConcurrentHashMap<>();

            public BlockLiteral literalOf(final BlockLabel blockLabel) {
                return new BlockLiteral(typeSystem.getBlockType(), blockLabel);
            }

            public BooleanLiteral literalOf(final boolean value) {
                return value ? TRUE : FALSE;
            }

            public FloatLiteral literalOf(final float value) {
                FloatLiteral v = new FloatLiteral(typeSystem.getFloat32Type(), value);
                return floatLiterals.computeIfAbsent(v, Function.identity());
            }

            public FloatLiteral literalOf(final double value) {
                FloatLiteral v = new FloatLiteral(typeSystem.getFloat64Type(), value);
                return floatLiterals.computeIfAbsent(v, Function.identity());
            }

            public IntegerLiteral literalOf(final long value) {
                IntegerLiteral v = new IntegerLiteral(typeSystem.getSignedInteger64Type(), value);
                return integerLiterals.computeIfAbsent(v, Function.identity());
            }

            public IntegerLiteral literalOf(final int value) {
                IntegerLiteral v = new IntegerLiteral(typeSystem.getSignedInteger32Type(), value);
                return integerLiterals.computeIfAbsent(v, Function.identity());
            }

            public IntegerLiteral literalOf(final short value) {
                IntegerLiteral v = new IntegerLiteral(typeSystem.getSignedInteger16Type(), value);
                return integerLiterals.computeIfAbsent(v, Function.identity());
            }

            public IntegerLiteral literalOf(final byte value) {
                IntegerLiteral v = new IntegerLiteral(typeSystem.getSignedInteger8Type(), value);
                return integerLiterals.computeIfAbsent(v, Function.identity());
            }

            public IntegerLiteral literalOf(final char value) {
                IntegerLiteral v = new IntegerLiteral(typeSystem.getUnsignedInteger16Type(), value);
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

            public DefinedConstantLiteral literalOfDefinedConstant(final Value constantValue) {
                return new DefinedConstantLiteral(constantValue);
            }

            public ObjectLiteral literalOf(final VmObject value) {
                Assert.checkNotNullParam("value", value);
                // todo: cache on object itself?
                return new ObjectLiteral(typeSystem.getReferenceType(value.getObjectType()).asConst(), value);
            }

            public MethodHandleLiteral literalOfMethodHandle(int referenceKind, int referenceIndex) {
                return new MethodHandleLiteral(typeSystem.getMethodHandleType(), referenceKind, referenceIndex);
            }

            public MethodDescriptorLiteral literalOfMethodDescriptor(String descriptor) {
                return new MethodDescriptorLiteral(typeSystem.getMethodDescriptorType(), descriptor);
            }

            public ValueArrayTypeIdLiteral literalOfArrayType(final ValueType elementType) {
                Assert.checkNotNullParam("elementType", elementType);
                if (elementType instanceof ReferenceType) {
                    throw new IllegalArgumentException("Value arrays may not contain references");
                }
                return primitiveArrayLiterals.computeIfAbsent(elementType, t -> new ValueArrayTypeIdLiteral(baseClassLiteral, typeSystem.getTypeIdType(), t));
            }

            public ReferenceArrayTypeIdLiteral literalOfArrayType(final ReferenceType elementType) {
                Assert.checkNotNullParam("elementType", elementType);
                return arrayObjectTypeIdLiterals.computeIfAbsent(elementType.getUpperBound(), t -> new ReferenceArrayTypeIdLiteral(baseClassLiteral, typeSystem.getTypeIdType(), elementType));
            }

            public ClassTypeIdLiteral baseClassLiteral() {
                return baseClassLiteral;
            }

            public ClassTypeIdLiteral literalOfClass(final String className, final ClassTypeIdLiteral superClass, final InterfaceTypeIdLiteral... interfaces) {
                Assert.checkNotNullParam("interfaces", interfaces);
                return new ClassTypeIdLiteral(className, superClass, interfaces, typeSystem.getTypeIdType());
            }

            public InterfaceTypeIdLiteral literalOfInterface(final String interfaceName, final InterfaceTypeIdLiteral... interfaces) {
                Assert.checkNotNullParam("interfaces", interfaces);
                return new InterfaceTypeIdLiteral(interfaceName, baseClassLiteral, interfaces, typeSystem.getTypeIdType());
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
        };
    }
}
