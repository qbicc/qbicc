package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 * A read of an instance field.
 */
public final class InstanceFieldRead extends AbstractValue implements FieldRead, InstanceOperation {
    private final Node dependency;
    private final Value instance;
    private final FieldElement fieldElement;
    private final JavaAccessMode mode;

    InstanceFieldRead(final int line, final int bci, final Node dependency, final Value instance, final FieldElement fieldElement, final JavaAccessMode mode) {
        super(line, bci);
        this.dependency = dependency;
        this.instance = instance;
        this.fieldElement = fieldElement;
        this.mode = mode;
    }

    public FieldElement getFieldElement() {
        return fieldElement;
    }

    public JavaAccessMode getMode() {
        return mode;
    }

    public Value getInstance() {
        return instance;
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
