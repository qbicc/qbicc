package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class PointerStore extends AbstractNode implements Action, OrderedNode {
    private final Node dependency;
    private final Value pointer;
    private final Value value;
    private final MemoryAccessMode accessMode;
    private final MemoryAtomicityMode atomicityMode;

    PointerStore(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.pointer = pointer;
        this.value = value;
        this.accessMode = accessMode;
        this.atomicityMode = atomicityMode;
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

    public MemoryAccessMode getAccessMode() {
        return accessMode;
    }

    public MemoryAtomicityMode getAtomicityMode() {
        return atomicityMode;
    }

    int calcHashCode() {
        return Objects.hash(dependency, pointer, value, accessMode, atomicityMode);
    }

    public boolean equals(final Object other) {
        return other instanceof PointerStore && equals((PointerStore) other);
    }

    public boolean equals(final PointerStore other) {
        return other == this || other != null && dependency.equals(other.dependency) && pointer.equals(other.pointer)
            && value.equals(other.value) && accessMode == other.accessMode && atomicityMode == other.atomicityMode;
    }

    public int getValueDependencyCount() {
        return 2;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? pointer : index == 1 ? value : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
