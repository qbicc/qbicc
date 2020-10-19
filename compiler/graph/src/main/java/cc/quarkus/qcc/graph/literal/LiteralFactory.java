package cc.quarkus.qcc.graph.literal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.interpreter.JavaObject;
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

    ObjectLiteral literalOf(JavaObject value);

    ValueArrayTypeIdLiteral literalOfArrayType(ValueType elementType);

    ReferenceArrayTypeIdLiteral literalOfArrayType(ReferenceType elementType);

    ClassTypeIdLiteral baseClassLiteral();

    ClassTypeIdLiteral literalOfClass(String className, ClassTypeIdLiteral superClass, InterfaceTypeIdLiteral... interfaces);

    InterfaceTypeIdLiteral literalOfInterface(String interfaceName, InterfaceTypeIdLiteral... interfaces);

    static LiteralFactory create(TypeSystem typeSystem) {
        return new LiteralFactory() {
            private final BooleanLiteral TRUE = new BooleanLiteral(typeSystem.getBooleanType().asConst(), true);
            private final BooleanLiteral FALSE = new BooleanLiteral(typeSystem.getBooleanType().asConst(), false);
            private final NullLiteral NULL = new NullLiteral(typeSystem.getNullType());
            private final ClassTypeIdLiteral baseClassLiteral = new ClassTypeIdLiteral("java/lang/Object", null, InterfaceTypeIdLiteral.NONE, typeSystem.getTypeIdType());
            private final ConcurrentMap<ValueType, ValueArrayTypeIdLiteral> primitiveArrayLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<TypeIdLiteral, ReferenceArrayTypeIdLiteral> arrayObjectTypeIdLiterals = new ConcurrentHashMap<>();
            private final ConcurrentMap<String, StringLiteral> stringLiterals = new ConcurrentHashMap<>();

            public BlockLiteral literalOf(final BlockLabel blockLabel) {
                return new BlockLiteral(typeSystem.getBlockType(), blockLabel);
            }

            public BooleanLiteral literalOf(final boolean value) {
                return value ? TRUE : FALSE;
            }

            public FloatLiteral literalOf(final float value) {
                return new FloatLiteral(typeSystem.getFloat32Type(), value);
            }

            public FloatLiteral literalOf(final double value) {
                return new FloatLiteral(typeSystem.getFloat64Type(), value);
            }

            public IntegerLiteral literalOf(final long value) {
                return new IntegerLiteral(typeSystem.getSignedInteger64Type(), value);
            }

            public IntegerLiteral literalOf(final int value) {
                return new IntegerLiteral(typeSystem.getSignedInteger32Type(), value);
            }

            public IntegerLiteral literalOf(final short value) {
                return new IntegerLiteral(typeSystem.getSignedInteger16Type(), value);
            }

            public IntegerLiteral literalOf(final byte value) {
                return new IntegerLiteral(typeSystem.getSignedInteger8Type(), value);
            }

            public IntegerLiteral literalOf(final char value) {
                return new IntegerLiteral(typeSystem.getUnsignedInteger16Type(), value);
            }

            public StringLiteral literalOf(final String value) {
                return stringLiterals.computeIfAbsent(value, v -> new StringLiteral(typeSystem.getStringType(), v));
            }

            public NullLiteral literalOfNull() {
                return NULL;
            }

            public ObjectLiteral literalOf(final JavaObject value) {
                Assert.checkNotNullParam("value", value);
                // todo: cache on object itself?
                return new ObjectLiteral(typeSystem.getReferenceType(value.getObjectType()).asConst(), value);
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
                return arrayObjectTypeIdLiterals.computeIfAbsent(elementType.getUpperBound(), t -> new ReferenceArrayTypeIdLiteral(baseClassLiteral, typeSystem.getTypeIdType(), elementType, null));
            }

            public ClassTypeIdLiteral baseClassLiteral() {
                return baseClassLiteral;
            }

            public ClassTypeIdLiteral literalOfClass(final String className, final ClassTypeIdLiteral superClass, final InterfaceTypeIdLiteral... interfaces) {
                Assert.checkNotNullParam("superClass", superClass);
                Assert.checkNotNullParam("interfaces", interfaces);
                return new ClassTypeIdLiteral(className, superClass, interfaces, typeSystem.getTypeIdType());
            }

            public InterfaceTypeIdLiteral literalOfInterface(final String interfaceName, final InterfaceTypeIdLiteral... interfaces) {
                Assert.checkNotNullParam("interfaces", interfaces);
                return new InterfaceTypeIdLiteral(interfaceName, interfaces, typeSystem.getTypeIdType());
            }
        };
    }
}
