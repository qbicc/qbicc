package org.qbicc.graph;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.qbicc.type.definition.element.ExecutableElement;

public class Fence extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private MemoryAtomicityMode atomicityMode;
    private Set<Node> incoming = Set.of();

    Fence(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final MemoryAtomicityMode atomicityMode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.atomicityMode = atomicityMode;
    }

    public MemoryAtomicityMode getAtomicityMode() {
        return atomicityMode;
    }

    public void setAtomicityMode(MemoryAtomicityMode mode) {
        atomicityMode = mode;
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

    public void setIncoming(Node from) {
        if (from != null) {
            if (incoming.isEmpty()) {
                incoming = Set.of(from);
            } else if (incoming.size() == 1) {
                if (! incoming.contains(from)) {
                    incoming = Set.of(from, incoming.iterator().next());
                }
            } else if (incoming.size() == 2) {
                Set<Node> old = this.incoming;
                incoming = new HashSet<>();
                incoming.addAll(old);
                incoming.add(from);
            } else {
                incoming.add(from);
            }
        }
    }

    public Set<Node> getIncoming() {
        return incoming;
    }
}
