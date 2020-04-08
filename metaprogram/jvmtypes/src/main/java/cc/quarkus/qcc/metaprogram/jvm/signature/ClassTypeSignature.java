package cc.quarkus.qcc.metaprogram.jvm.signature;

import cc.quarkus.qcc.metaprogram.jvm.PackageName;

/**
 * Some class (or interface).  Does not specify whether the class is accessed by reference or by value.
 */
public interface ClassTypeSignature extends ReferenceTypeSignature {
    /**
     * The simple name of the class.
     *
     * @return the simple name
     */
    String getSimpleName();

    /**
     * Determine whether this class reference type has a package name.  If {@link #hasEnclosing()} returns {@code true},
     * this method will always return {@code false}.
     *
     * @return {@code true} if this type has a package name, or {@code false} if it resides in the default package or
     * within an enclosing class
     */
    boolean hasPackageName();

    /**
     * Get the package name.
     *
     * @return the package name
     * @throws IllegalArgumentException if the class does not have a package name
     */
    PackageName getPackageName() throws IllegalArgumentException;

    /**
     * Determine whether this class reference type has an enclosing class reference type.  If {@link #hasPackageName()}
     * returns {@code true}, this method will always return {@code false}.
     *
     * @return {@code true} if this type is enclosed in a class, or {@code false} if it resides in a package or in the
     * default package
     */
    boolean hasEnclosing();

    /**
     * Get the enclosing class type.
     *
     * @return the enclosing class type
     */
    ClassTypeSignature getEnclosing() throws IllegalArgumentException;

    /**
     * Get the number of type arguments.  This should normally match the number of parameters on the declaring type, but
     * sometimes might not if there was a source-incompatible change to that type or if this signature refers to a raw
     * type.
     *
     * @return the number of type arguments
     */
    int getTypeArgumentCount();

    /**
     * Get the type argument at the given index.
     *
     * @return the type argument at the given index
     */
    TypeArgument getTypeArgument(int index) throws IndexOutOfBoundsException;

    /**
     * Any kind of type argument.
     */
    interface TypeArgument {}

    /**
     * The explicit "any" type argument, encoded as {@code *}.
     */
    interface AnyTypeArgument extends TypeArgument {
        AnyTypeArgument INSTANCE = new AnyTypeArgument() {};
    }

    /**
     * A type argument that is a bound of some sort.
     */
    interface BoundTypeArgument extends TypeArgument {
        /**
         * Get the variance of the bound.
         *
         * @return the variance of the bound
         */
        Variance getVariance();

        /**
         * Get the type of the bound.
         *
         * @return the type of the bound
         */
        ReferenceTypeSignature getValue();
    }

    enum Variance {
        CONTRAVARIANT,
        INVARIANT,
        COVARIANT,
        ;
    }
}
