package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A load from memory.
 */
public class Load extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final PointerValue handle;
    private final ReadAccessMode mode;

    Load(Node callSite, ExecutableElement element, int line, int bci, Node dependency, PointerValue handle, ReadAccessMode mode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.handle = handle;
        this.mode = mode;
        if (! handle.isReadable()) {
            throw new IllegalArgumentException("Handle is not readable");
        }
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    int calcHashCode() {
        return Objects.hash(dependency, handle, mode);
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

    public boolean equals(final Load other) {
        return this == other || other != null && dependency.equals(other.dependency) && handle.equals(other.handle) && mode == other.mode;
    }

    public ValueType getType() {
        return handle.getPointeeType();
    }

    public ReadAccessMode getAccessMode() {
        return mode;
    }

    @Override
    public boolean hasPointerValueDependency() {
        return true;
    }

    @Override
    public PointerValue getPointerValue() {
        return handle;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isConstant() {
        return handle.isValueConstant();
    }
}
