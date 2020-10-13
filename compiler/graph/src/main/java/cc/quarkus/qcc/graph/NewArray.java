package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A {@code new} allocation operation for array objects.
 */
public final class NewArray extends AbstractValue {
    private final Node dependency;
    private final ReferenceType type;
    private final Literal elementTypeId;
    private final Value size;

    NewArray(final Node dependency, final Literal elementTypeId, final ReferenceType type, final Value size) {
        this.dependency = dependency;
        this.elementTypeId = elementTypeId;
        this.type = type;
        this.size = size;
    }

    public ValueType getType() {
        return type;
    }

    public Value getSize() {
        return size;
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
        return 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? size : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
