package org.qbicc.type;

/**
 * The type representing method descriptor literals, which are always {@code const}, have no size and are incomplete.
 */
public final class MethodDescriptorType extends ValueType {
  MethodDescriptorType(final TypeSystem typeSystem) {
        super(typeSystem, MethodDescriptorType.class.hashCode());
    }

    public boolean isComplete() {
        return false;
    }

    public long getSize() {
        return 0;
    }

    public int getAlign() {
        return 0;
    }

    public boolean equals(final ValueType other) {
        return other instanceof MethodDescriptorType && super.equals(other);
    }

    public StringBuilder toString(final StringBuilder b) {
        return b.append("methoddescriptor");
    }

    public StringBuilder toFriendlyString(final StringBuilder b) {
        return b.append("mdesc");
    }
}
