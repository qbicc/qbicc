package org.qbicc.graph;

import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class StackAllocation extends AbstractValue {
    private final ValueType type;
    private final Value count;
    private final Value align;

    StackAllocation(final Node callSite, final ExecutableElement element, final int line, final int bci, final ValueType type, final Value count, final Value align) {
        super(callSite, element, line, bci);
        this.type = type;
        this.count = count;
        this.align = align;
    }

    int calcHashCode() {
        // every instance is a unique allocation by definition
        return System.identityHashCode(this);
    }

    public boolean equals(final Object other) {
        return other == this;
    }

    public PointerType getType() {
        return type.getPointer();
    }

    public Value getCount() {
        return count;
    }

    public Value getAlign() {
        return align;
    }

    public int getValueDependencyCount() {
        return 2;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? count : index == 1 ? align : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
