package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

public class Fence extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final MemoryAtomicityMode atomicityMode;

    Fence(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final MemoryAtomicityMode atomicityMode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.atomicityMode = atomicityMode;
    }

    public MemoryAtomicityMode getAtomicityMode() {
        return atomicityMode;
    }

    int calcHashCode() {
        return Objects.hash(Fence.class, dependency, atomicityMode);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public boolean equals(Object other) {
        return other instanceof Fence && equals((Fence) other);
    }

    public boolean equals(final Fence other) {
        return this == other || other != null
               && dependency.equals(other.dependency)
               && atomicityMode == other.atomicityMode;
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
