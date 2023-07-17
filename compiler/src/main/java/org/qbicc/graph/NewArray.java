package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;

/**
 * A {@code new} allocation operation for array objects.
 */
public final class NewArray extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final PrimitiveArrayObjectType type;
    private final Value size;

    NewArray(final ProgramLocatable pl, Node dependency, final PrimitiveArrayObjectType type, final Value size) {
        super(pl);
        this.dependency = dependency;
        this.type = type;
        this.size = size;
    }

    public Node getDependency() {
        return dependency;
    }

    public boolean maySafePoint() {
        return true;
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

    public PrimitiveArrayObjectType getArrayType() {
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

    @Override
    String getNodeName() {
        return "NewArray";
    }

    public boolean equals(final Object other) {
        return this == other;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        type.toString(b);
        b.append(',');
        size.toReferenceString(b);
        b.append(')');
        return b;
    }

    @Override
    public boolean isNullable() {
        return false;
    }
}
