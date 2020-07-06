package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public interface Type extends Node {

    // TODO: Void isn't really a type...
    VoidType VOID = new VoidTypeImpl();

    NullType NULL_TYPE = new NullTypeImpl();

    static ArrayType arrayOf(Type elementType) {
        return new ArrayTypeImpl(elementType);
    }

    @Deprecated
    static ClassType classNamed(String name) {
        throw Assert.unsupported();
    }

    static ClassType classType(VerifiedTypeDefinition definition, ClassType superType, InterfaceType... interfaceTypes) {
        return new ClassTypeImpl(definition, superType, interfaceTypes.length == 0 ? InterfaceType.NO_INTERFACES : interfaceTypes.clone());
    }

    static InterfaceType interfaceType(VerifiedTypeDefinition definition, InterfaceType... interfaceTypes) {
        return new InterfaceTypeImpl(definition, interfaceTypes.length == 0 ? InterfaceType.NO_INTERFACES : interfaceTypes.clone());
    }

    boolean isAssignableFrom(Type otherType);

    int getParameterCount();

    String getParameterName(int index) throws IndexOutOfBoundsException;

    Constraint getParameterConstraint(int index) throws IndexOutOfBoundsException;

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

    ArrayClassType JAVA_BYTE_ARRAY = new ArrayClassTypeImpl(S8);
    ArrayClassType JAVA_SHORT_ARRAY = new ArrayClassTypeImpl(S16);
    ArrayClassType JAVA_INT_ARRAY = new ArrayClassTypeImpl(S32);
    ArrayClassType JAVA_LONG_ARRAY = new ArrayClassTypeImpl(S64);

    ArrayClassType JAVA_CHAR_ARRAY = new ArrayClassTypeImpl(U16);

    ArrayClassType JAVA_FLOAT_ARRAY = new ArrayClassTypeImpl(F32);
    ArrayClassType JAVA_DOUBLE_ARRAY = new ArrayClassTypeImpl(F64);

    ArrayClassType JAVA_BOOLEAN_ARRAY = new ArrayClassTypeImpl(BOOL);

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

    /**
     * Get the zero value for this type.
     *
     * @return the zero value for this type
     */
    default Value zero() {
        return Value.const_(0).withTypeRaw(this);
    }
}
