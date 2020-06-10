package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
public interface Type extends Node {

    // TODO: Void isn't really a type...
    VoidType VOID = new VoidTypeImpl();

    static ArrayType arrayOf(Type elementType) {
        return new ArrayTypeImpl(elementType);
    }

    static ClassType classNamed(String name) {
        return new ClassTypeImpl(name);
    }

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
