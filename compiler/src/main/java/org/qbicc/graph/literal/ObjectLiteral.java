package org.qbicc.graph.literal;

import java.util.Objects;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.graph.ValueVisitorLong;
import org.qbicc.interpreter.VmObject;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.ReferenceType;

/**
 *
 */
public final class ObjectLiteral extends WordLiteral {
    private final ReferenceType type;
    private final VmObject value;
    private final int hashCode;

    ObjectLiteral(final ReferenceType type, final VmObject value) {
        this.type = type;
        this.value = value;
        hashCode = Objects.hash(type, value);
    }

    public ReferenceType getType() {
        return type;
    }

    public PhysicalObjectType getObjectType() {
        return value.getObjectType();
    }

    public VmObject getValue() {
        return value;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isZero() {
        return false;
    }

    public boolean isNullable() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof ObjectLiteral && equals((ObjectLiteral) other);
    }

    public boolean equals(final ObjectLiteral other) {
        return this == other || other != null && type.equals(other.type) && value.equals(other.value);
    }

    public int hashCode() {
        return hashCode;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append("object").append('(').append(value).append(')');
    }
}
