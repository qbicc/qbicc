package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;

/**
 *
 */
public interface Type {

    Type[] NO_TYPES = new Type[0];

    // TODO: Void isn't really a type...
    VoidType VOID = new VoidTypeImpl();

    NullType NULL_TYPE = new NullTypeImpl();

    ArrayClassType getArrayClassType();

    static ClassType classType(VerifiedTypeDefinition definition, ClassType superType, InterfaceType... interfaceTypes) {
        return new ClassTypeImpl(definition, superType, interfaceTypes.length == 0 ? InterfaceType.NO_INTERFACES : interfaceTypes.clone());
    }

    static InterfaceType interfaceType(VerifiedTypeDefinition definition, InterfaceType... interfaceTypes) {
        return new InterfaceTypeImpl(definition, interfaceTypes.length == 0 ? InterfaceType.NO_INTERFACES : interfaceTypes.clone());
    }

    boolean isAssignableFrom(Type otherType);

    Type RETURN_ADDRESS = ReturnAddressType.INSTANCE;

    WordType BOOL = new BooleanTypeImpl();

    SignedIntegerType S8 = new SignedInteger8TypeImpl();
    SignedIntegerType S16 = new SignedInteger16TypeImpl();
    SignedIntegerType S32 = new SignedInteger32TypeImpl();
    SignedIntegerType S64 = new SignedInteger64TypeImpl();
    UnsignedIntegerType U8 = new UnsignedInteger8TypeImpl();
    UnsignedIntegerType U16 = new UnsignedInteger16TypeImpl();
    UnsignedIntegerType U32 = new UnsignedInteger32TypeImpl();
    UnsignedIntegerType U64 = new UnsignedInteger64TypeImpl();

    FloatType F32 = new Float32Type();
    FloatType F64 = new Float64Type();

    StringLiteralType STRING = new StringLiteralTypeImpl();

    ClassLiteralType CLASS = new ClassLiteralTypeImpl();

    // primitive array types

    ArrayClassType JAVA_BYTE_ARRAY = S8.getArrayClassType();
    ArrayClassType JAVA_SHORT_ARRAY = S16.getArrayClassType();
    ArrayClassType JAVA_INT_ARRAY = S32.getArrayClassType();
    ArrayClassType JAVA_LONG_ARRAY = S64.getArrayClassType();

    ArrayClassType JAVA_CHAR_ARRAY = U16.getArrayClassType();

    ArrayClassType JAVA_FLOAT_ARRAY = F32.getArrayClassType();
    ArrayClassType JAVA_DOUBLE_ARRAY = F64.getArrayClassType();

    ArrayClassType JAVA_BOOLEAN_ARRAY = BOOL.getArrayClassType();

    default boolean isClass2Type() {
        return false;
    }

    default boolean isZero(long value) {
        return value == 0;
    }

    default boolean isZero(int value) {
        return value == 0;
    }

    default boolean isOne(int value) {
        return value == 1;
    }

    default boolean isOne(long value) {
        return value == 1;
    }

    default boolean isOne(byte[] value) {
        if (value[0] != 1) {
            return false;
        }
        for (int i = 1; i < value.length; i ++) {
            if (value[i] != 0) {
                return false;
            }
        }
        return true;
    }

    default boolean isSigned() {
        return false;
    }

    default boolean isUnsigned() {
        return false;
    }

    /**
     * Get the zero value for this type.
     *
     * @return the zero value for this type
     */
    default Value zero() {
        return Value.const_(0).withTypeRaw(this);
    }

    // Interpreter only - temporary?

    /**
     * Used only by the interpreter.  Box the given value according to its type.
     *
     * @param value the constant value
     * @return the boxed value
     */
    default Object boxValue(ConstantValue value) {
        throw new UnsupportedOperationException();
    }

    default Object interpAdd(Object v1, Object v2) {
        return null;
    }
}
