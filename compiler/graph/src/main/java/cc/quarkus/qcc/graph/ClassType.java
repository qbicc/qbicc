package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;

/**
 * A class (or interface) type.  If a class (or interface) with a given name exists in more than one class loader,
 * each one will have a corresponding distinct instance of this type.
 */
public interface ClassType extends Type {
    // todo: compact package and class names
    String getClassName();

    ClassType getSuperClass();

    default boolean hasSuperClass() {
        return getSuperClass() != null;
    }

    int getInterfaceCount();

    VerifiedTypeDefinition getDefinition();

    InterfaceType getInterface(int index) throws IndexOutOfBoundsException;

    boolean isSuperTypeOf(ClassType other);

    default boolean isAssignableFrom(ClassType otherType) {
        return otherType == this || otherType instanceof EitherType && ((EitherType) otherType).bothAreAssignableTo(this);
    }

    /**
     * Get the type that is an array class of this type.
     *
     * @return the type that is an array of this type
     */
    ArrayClassType getArrayType();

    static ClassType highest(ClassType t1, ClassType t2) {
        if (t1.isSuperTypeOf(t2)) {
            return t1;
        } else if (t2.isSuperTypeOf(t1)) {
            return t2;
        } else {
            assert t1.hasSuperClass() && t2.hasSuperClass();
            return highest(t2, t1.getSuperClass());
        }
    }

    default boolean isAssignableFrom(Type otherType) {
        return otherType instanceof ClassType && isAssignableFrom((ClassType) otherType)
            || otherType instanceof EitherType && ((EitherType) otherType).bothAreAssignableTo(this);
    }

    default Value zero() {
        throw new UnsupportedOperationException();
    }
}
