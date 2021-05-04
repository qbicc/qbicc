package org.qbicc.type;

/**
 * A word type that can contain a {@code null} value.
 */
public abstract class NullableType extends WordType {
    NullableType(final TypeSystem typeSystem, final int hashCode) {
        super(typeSystem, hashCode);
    }
}
