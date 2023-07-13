package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public class Store extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final Value pointer;
    private final Value value;
    private final WriteAccessMode mode;

    Store(Node callSite, ExecutableElement element, int line, int bci, Node dependency, Value pointer, Value value, WriteAccessMode mode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.pointer = pointer;
        this.value = value;
        this.mode = mode;
        if (! pointer.isWritable()) {
            throw new IllegalArgumentException("Handle is not writable");
        }
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public Value getPointer() {
        return pointer;
    }

    public Value getValue() {
        return value;
    }

    public WriteAccessMode getAccessMode() {
        return mode;
    }

    int calcHashCode() {
        return Objects.hash(dependency, pointer, value, mode);
    }

    @Override
    String getNodeName() {
        return "Store";
    }

    public boolean equals(final Object other) {
        return other instanceof Store && equals((Store) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        pointer.toReferenceString(b);
        b.append(',');
        value.toReferenceString(b);
        b.append(',');
        b.append(mode);
        b.append(')');
        return b;
    }

    public boolean equals(final Store other) {
        return this == other || other != null && dependency.equals(other.dependency) && pointer.equals(other.pointer) && value.equals(other.value) && mode == other.mode;
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> pointer;
            case 1 -> value;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public <T, R> R accept(ActionVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
