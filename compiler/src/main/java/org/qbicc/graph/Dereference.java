package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.ValueType;

/**
 * An opaque member selection.  The wrapped value is a pointer to any memory object.
 * Selected members may be unwrapped to be used as lvalues, or may be lazily transformed to plain loads.
 */
public final class Dereference extends AbstractValue {
    private final Value pointer;

    Dereference(final ProgramLocatable pl, Value pointer) {
        super(pl);
        this.pointer = pointer;
    }

    public Value getPointer() {
        return pointer;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> pointer;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    public ValueType getType() {
        return pointer.getPointeeType();
    }

    int calcHashCode() {
        return Objects.hash(Dereference.class, pointer);
    }

    @Override
    String getNodeName() {
        return "Dereference";
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        return pointer.toReferenceString(b.append("deref "));
    }

    public boolean equals(final Object other) {
        return other instanceof Dereference && equals((Dereference) other);
    }

    public boolean equals(final Dereference other) {
        return this == other || other != null && pointer.equals(other.pointer);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}