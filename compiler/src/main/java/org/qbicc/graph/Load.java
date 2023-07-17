package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.type.ValueType;

/**
 * A load from memory.
 */
public class Load extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final Value pointer;
    private final ReadAccessMode mode;

    Load(final ProgramLocatable pl, Node dependency, Value pointer, ReadAccessMode mode) {
        super(pl);
        this.dependency = dependency;
        this.pointer = pointer;
        this.mode = mode;
        if (! pointer.isReadable()) {
            throw new IllegalArgumentException("Pointer is not readable");
        }
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    int calcHashCode() {
        return Objects.hash(dependency, pointer, mode);
    }

    @Override
    String getNodeName() {
        return "Load";
    }

    public boolean equals(final Object other) {
        return other instanceof Load && equals((Load) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return super.toString(b).append('(').append(mode).append(')');
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        return pointer.toReferenceString(b.append("load ").append(mode).append(" from "));
    }

    public boolean equals(final Load other) {
        return this == other || other != null && dependency.equals(other.dependency) && pointer.equals(other.pointer) && mode == other.mode;
    }

    public ValueType getType() {
        return pointer.getPointeeType();
    }

    public Value getPointer() {
        return pointer;
    }

    public ReadAccessMode getAccessMode() {
        return mode;
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

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isNullable() {
        return pointer.isPointeeNullable();
    }

    public boolean isConstant() {
        return pointer.isPointeeConstant();
    }
}
