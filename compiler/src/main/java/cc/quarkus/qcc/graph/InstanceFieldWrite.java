package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 * A write of an instance field.
 */
public final class InstanceFieldWrite extends AbstractNode implements FieldWrite, InstanceOperation, Action {
    private final Node dependency;
    private final Value instance;
    private final FieldElement fieldElement;
    private final Value value;
    private final JavaAccessMode mode;

    InstanceFieldWrite(final int line, final int bci, final Node dependency, final Value instance, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        super(line, bci);
        this.dependency = dependency;
        this.instance = instance;
        this.fieldElement = fieldElement;
        this.value = value;
        this.mode = mode;
    }

    public FieldElement getFieldElement() {
        return fieldElement;
    }

    public Value getWriteValue() {
        return value;
    }

    public Value getInstance() {
        return instance;
    }

    public JavaAccessMode getMode() {
        return mode;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public int getValueDependencyCount() {
        return 2;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInstance() : index == 1 ? getWriteValue() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(dependency, instance, fieldElement, value, mode);
    }

    public boolean equals(final Object other) {
        return other instanceof InstanceFieldWrite && equals((InstanceFieldWrite) other);
    }

    public boolean equals(final InstanceFieldWrite other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && instance.equals(other.instance)
            && fieldElement.equals(other.fieldElement)
            && value.equals(other.value)
            && mode.equals(other.mode);
    }
}
