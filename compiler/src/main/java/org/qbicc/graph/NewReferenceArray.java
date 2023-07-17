package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;

/**
 * A {@code new} allocation operation for reference array objects.
 *
 * Note that the type field represents the compile-time type of the array, while the elemTypeId and dimensions
 * represent the actual runtime type of the value.  These will be identical for NewReferenceArray nodes that
 * were originally created via `anewarray`, but may be different for nodes created via emitNewReferenceArray
 * (whose static type is Object[], but dynamic type may be any reference array).
 */
public final class NewReferenceArray extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final ReferenceArrayObjectType type;
    private final Value elemTypeId;
    private final Value dimensions;
    private final Value size;

    NewReferenceArray(final ProgramLocatable pl, Node dependency, final ReferenceArrayObjectType type,
                      final Value elemTypeId, final Value dimensions, final Value size) {
        super(pl);
        this.dependency = dependency;
        this.type = type;
        this.elemTypeId = elemTypeId;
        this.dimensions = dimensions;
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

    public Value getElemTypeId() {
        return elemTypeId;
    }

    public Value getDimensions() {
        return dimensions;
    }

    public Value getSize() {
        return size;
    }

    public ValueType getElementType() {
        return type.getElementType();
    }

    public ReferenceArrayObjectType getArrayType() {
        return type;
    }

    public int getValueDependencyCount() {
        return 3;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        switch(index) {
            case 0: return elemTypeId;
            case 1: return dimensions;
            case 2: return size;
            default: throw new IndexOutOfBoundsException(index);
        }
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return System.identityHashCode(this);
    }

    @Override
    String getNodeName() {
        return "NewReferenceArray";
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
        elemTypeId.toReferenceString(b);
        b.append(',');
        dimensions.toReferenceString(b);
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
