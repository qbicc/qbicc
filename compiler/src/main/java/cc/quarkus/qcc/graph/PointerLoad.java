package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.Element;

/**
 * A pointer load operation.
 */
public final class PointerLoad extends AbstractValue {
    private final Node dependency;
    private final Value pointer;
    private final MemoryAccessMode accessMode;
    private final MemoryAtomicityMode atomicityMode;

    PointerLoad(final Element element, final int line, final int bci, final Node dependency, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        super(element, line, bci);
        this.dependency = dependency;
        this.pointer = pointer;
        this.accessMode = accessMode;
        this.atomicityMode = atomicityMode;
    }

    public Node getDependency() {
        return dependency;
    }

    public Value getPointer() {
        return pointer;
    }

    public MemoryAccessMode getAccessMode() {
        return accessMode;
    }

    public MemoryAtomicityMode getAtomicityMode() {
        return atomicityMode;
    }

    int calcHashCode() {
        return Objects.hash(dependency, pointer, accessMode, atomicityMode);
    }

    public boolean equals(final Object other) {
        return other instanceof PointerLoad && equals((PointerLoad) other);
    }

    public boolean equals(final PointerLoad other) {
        return this == other || other != null && dependency.equals(other.dependency) && pointer.equals(other.pointer)
            && accessMode == other.accessMode && atomicityMode == other.atomicityMode;
    }

    public ValueType getType() {
        return (ValueType) ((PointerType)pointer.getType()).getPointeeType();
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? pointer : Util.throwIndexOutOfBounds(index);
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
