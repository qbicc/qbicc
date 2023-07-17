package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ReferenceType;

/**
 * A {@code new} allocation operation.
 *
 *  Note that the type field represents the compile-time type of the object, while the typeId
 *  represents the actual runtime type of the value.  These will be identical for New nodes that
 *  were originally created via `the new` bytecode, but may be different for nodes created via
 *  emitNew intrinsic (whose static type is Object, but dynamic type may be any Class instance).
 */
public final class New extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final ClassObjectType type;
    private final Value typeId;
    private final Value size;
    private final Value align;

    New(final ProgramLocatable pl, Node dependency, final ClassObjectType type,
        final Value typeId, final Value size, final Value align) {
        super(pl);
        this.dependency = dependency;
        this.type = type;
        this.typeId = typeId;
        this.size = size;
        this.align = align;
    }

    public Node getDependency() {
        return dependency;
    }

    public boolean maySafePoint() {
        return true;
    }

    public int getValueDependencyCount() {
        return 3;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        switch(index) {
            case 0: return typeId;
            case 1: return size;
            case 2: return align;
            default: throw new IndexOutOfBoundsException(index);
        }
    }

    public ReferenceType getType() {
        return type.getReference();
    }

    public Value getTypeId() {
        return typeId;
    }

    public Value getSize() {
        return size;
    }

    public Value getAlign() {
        return align;
    }

    public ClassObjectType getClassObjectType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        // every allocation is unique
        return System.identityHashCode(this);
    }

    @Override
    String getNodeName() {
        return "New";
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
        typeId.toReferenceString(b);
        b.append(',');
        size.toReferenceString(b);
        b.append(',');
        align.toReferenceString(b);
        b.append(')');
        return b;
    }

    @Override
    public boolean isDefNe(Value other) {
        return other instanceof NullLiteral;
    }

    @Override
    public boolean isNullable() {
        return false;
    }
}
