package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * A {@code new} allocation operation for array objects.
 */
public final class NewArray extends AbstractValue {
    private final ArrayObjectType type;
    private final Value size;

    NewArray(final Node callSite, final ExecutableElement element, final int line, final int bci, final ArrayObjectType type, final Value size) {
        super(callSite, element, line, bci);
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
        return System.identityHashCode(this);
    }

    public boolean equals(final Object other) {
        return this == other;
    }
}
