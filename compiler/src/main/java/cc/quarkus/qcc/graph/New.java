package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.ReferenceType;

/**
 * A {@code new} allocation operation.
 */
public final class New extends AbstractValue {
    private final Node dependency;
    private final ClassObjectType type;

    New(final int line, final int bci, final Node dependency, final ClassObjectType type) {
        super(line, bci);
        this.dependency = dependency;
        this.type = type;
    }

    public ReferenceType getType() {
        return type.getReference();
    }

    public ClassObjectType getClassObjectType() {
        return type;
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
        return Objects.hash(dependency, type);
    }

    public boolean equals(final Object other) {
        return other instanceof New && equals((New) other);
    }

    public boolean equals(final New other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && type.equals(other.type);
    }
}
