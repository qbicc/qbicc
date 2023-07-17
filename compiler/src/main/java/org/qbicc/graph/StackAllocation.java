package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;

/**
 *
 */
public final class StackAllocation extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final ValueType type;
    private final Value count;
    private final Value align;

    StackAllocation(final ProgramLocatable pl, Node dependency, final ValueType type, final Value count, final Value align) {
        super(pl);
        this.dependency = dependency;
        this.type = type;
        this.count = count;
        this.align = align;
    }

    int calcHashCode() {
        // every instance is a unique allocation by definition
        return System.identityHashCode(this);
    }

    @Override
    String getNodeName() {
        return "StackAllocation";
    }

    public boolean equals(final Object other) {
        return other == this;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        type.toString(b);
        b.append(',');
        count.toReferenceString(b);
        b.append(',');
        align.toReferenceString(b);
        b.append(')');
        return b;
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

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }
}
