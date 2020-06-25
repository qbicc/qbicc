package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
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

    static ClassType classType(String name, ClassType superType, InterfaceType... interfaceTypes) {
        return new ClassTypeImpl(name, superType, interfaceTypes.length == 0 ? InterfaceType.NO_INTERFACES : interfaceTypes.clone());
    }

    static InterfaceType interfaceType(String name, InterfaceType... interfaceTypes) {
        return new InterfaceTypeImpl(name, interfaceTypes.length == 0 ? InterfaceType.NO_INTERFACES : interfaceTypes.clone());
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

    // java.lang.Object

    ClassType JAVA_LANG_OBJECT = classType("java/lang/Object", null, InterfaceType.NO_INTERFACES);

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

}
