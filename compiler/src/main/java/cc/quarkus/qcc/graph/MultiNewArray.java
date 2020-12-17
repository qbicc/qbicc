package cc.quarkus.qcc.graph;

import java.util.List;
import java.util.Objects;

import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.type.ReferenceType;

/**
 * A {@code new} allocation operation for multi-dimensional array objects.
 */
public final class MultiNewArray extends AbstractValue {
    private final Node dependency;
    private final ReferenceType type;
    private final List<Value> dimensions;
    private final Literal elementTypeId;

    MultiNewArray(final int line, final int bci, final Node dependency, final Literal elementTypeId, final ReferenceType type, final List<Value> dimensions) {
        super(line, bci);
        this.dependency = dependency;
        this.elementTypeId = elementTypeId;
        this.type = type;
        this.dimensions = dimensions;
    }

    public ReferenceType getType() {
        return type;
    }

    public List<Value> getDimensions() {
        return dimensions;
    }

    public Literal getElementTypeId() {
        return elementTypeId;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public int getValueDependencyCount() {
        return dimensions.size();
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return dimensions.get(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(dependency, type, elementTypeId, dimensions);
    }

    public boolean equals(final Object other) {
        return other instanceof MultiNewArray && equals((MultiNewArray) other);
    }

    public boolean equals(final MultiNewArray other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && type.equals(other.type)
            && elementTypeId.equals(other.elementTypeId)
            && dimensions.equals(other.dimensions);
    }
}
