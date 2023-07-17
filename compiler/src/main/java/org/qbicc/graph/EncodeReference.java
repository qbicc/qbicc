package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;

/**
 * A node representing the encoding of a reference from a pointer value.
 * Reference encode nodes are ordered because an encoded reference is only guaranteed to refer to a valid pointer until the next safepoint event.
 */
public final class EncodeReference extends AbstractValue implements WordCastValue, OrderedNode {
    private final Node dependency;
    private final Value pointer;
    private final ReferenceType refType;

    EncodeReference(final ProgramLocatable pl, Node dependency, Value pointer, ReferenceType refType) {
        super(pl);
        // eagerly resolve dependency
        while (dependency instanceof OrderedNode on && ! on.maySafePoint()) {
            dependency = on.getDependency();
        }
        this.dependency = dependency;
        this.pointer = pointer;
        if (! (pointer.getType() instanceof PointerType)) {
            throw new IllegalArgumentException("Wrong input type for reference encode");
        }
        this.refType = refType;
    }

    @Override
    public boolean isNullable() {
        return pointer.isNullable();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(EncodeReference.class, dependency, pointer);
    }

    @Override
    String getNodeName() {
        return "EncodeReference";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EncodeReference dr && equals(dr);
    }

    public boolean equals(EncodeReference other) {
        return this == other || other != null && dependency.equals(other.dependency) && pointer.equals(other.pointer);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public Value getInput() {
        return pointer;
    }

    @Override
    public ReferenceType getType() {
        return refType;
    }

    @Override
    public PointerType getInputType() {
        return pointer.getType(PointerType.class);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        getInput().toReferenceString(b);
        b.append(')');
        b.append(" to ");
        refType.toString(b);
        return b;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
