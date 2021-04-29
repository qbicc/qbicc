package org.qbicc.graph;

import java.util.List;

import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A {@code new} allocation operation for multi-dimensional array objects.
 */
public final class MultiNewArray extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final ArrayObjectType type;
    private final List<Value> dimensions;

    MultiNewArray(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final ArrayObjectType type, final List<Value> dimensions) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.type = type;
        this.dimensions = dimensions;
    }

    public Node getDependency() {
        return dependency;
    }

    public ReferenceType getType() {
        return type.getReference();
    }

    public List<Value> getDimensions() {
        return dimensions;
    }

    public ValueType getElementType() {
        return type.getElementType();
    }

    public ArrayObjectType getArrayType() {
        return type;
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

    public boolean isNullable() {
        return false;
    }

    int calcHashCode() {
        return System.identityHashCode(this);
    }

    public boolean equals(final Object other) {
        return this == other;
    }
}
