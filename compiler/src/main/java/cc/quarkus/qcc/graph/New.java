package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A {@code new} allocation operation.
 */
public final class New extends AbstractValue {
    private final Node dependency;
    private final ReferenceType type;
    private final ClassTypeIdLiteral instanceTypeId;

    New(final int line, final int bci, final Node dependency, final ReferenceType type, final ClassTypeIdLiteral instanceTypeId) {
        super(line, bci);
        this.dependency = dependency;
        this.type = type;
        this.instanceTypeId = instanceTypeId;
    }

    public ValueType getType() {
        return type;
    }

    public ClassTypeIdLiteral getInstanceTypeId() {
        return instanceTypeId;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(dependency, type, instanceTypeId);
    }

    public boolean equals(final Object other) {
        return other instanceof New && equals((New) other);
    }

    public boolean equals(final New other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && type.equals(other.type)
            && instanceTypeId.equals(other.instanceTypeId);
    }
}
