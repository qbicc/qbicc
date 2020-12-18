package cc.quarkus.qcc.graph.literal;

import java.util.Objects;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.type.PhysicalObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;

/**
 *
 */
public final class ObjectLiteral extends Literal {
    private final ReferenceType type;
    private final VmObject value;
    private final int hashCode;

    ObjectLiteral(final ReferenceType type, final VmObject value) {
        this.type = type;
        this.value = value;
        hashCode = Objects.hash(type, value);
    }

    public ValueType getType() {
        return type;
    }

    public PhysicalObjectType getObjectType() {
        return value.getObjectType();
    }

    public VmObject getValue() {
        return value;
    }

    public Constraint getConstraint() {
        return null;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
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
}
