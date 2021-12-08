package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.type.definition.element.ExecutableElement;

public class Fence extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final GlobalAccessMode accessMode;

    Fence(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final GlobalAccessMode accessMode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.accessMode = accessMode;
    }

    public GlobalAccessMode getAccessMode() {
        return accessMode;
    }

    int calcHashCode() {
        return Objects.hash(Fence.class, dependency, accessMode);
    }

    @Override
    String getNodeName() {
        return "Fence";
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public boolean equals(Object other) {
        return other instanceof Fence && equals((Fence) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        b.append(accessMode);
        b.append(')');
        return b;
    }

    public boolean equals(final Fence other) {
        return this == other || other != null
               && dependency.equals(other.dependency)
               && accessMode == other.accessMode;
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
