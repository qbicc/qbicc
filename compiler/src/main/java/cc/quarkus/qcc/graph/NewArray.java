package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * A {@code new} allocation operation for array objects.
 */
public final class NewArray extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final ArrayObjectType type;
    private final Value size;

    NewArray(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final ArrayObjectType type, final Value size) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.type = type;
        this.size = size;
    }

    public ReferenceType getType() {
        return type.getReference();
    }

    public Value getSize() {
        return size;
    }

    public ValueType getElementType() {
        return type.getElementType();
    }

    public ArrayObjectType getArrayType() {
        return type;
    }

    @Override
    public Node getDependency() {
        return dependency;
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

    int calcHashCode() {
        return Objects.hash(dependency, type, size);
    }

    public boolean equals(final Object other) {
        return other instanceof NewArray && equals((NewArray) other);
    }

    public boolean equals(final NewArray other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && type.equals(other.type)
            && size.equals(other.size);
    }
}
