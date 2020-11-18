package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 *
 */
public final class StaticFieldRead extends AbstractValue implements FieldRead {
    private final Node dependency;
    private final FieldElement fieldElement;
    private final JavaAccessMode mode;

    StaticFieldRead(final int line, final int bci, final Node dependency, final FieldElement fieldElement, final JavaAccessMode mode) {
        super(line, bci);
        this.dependency = dependency;
        this.fieldElement = fieldElement;
        this.mode = mode;
    }

    public FieldElement getFieldElement() {
        return fieldElement;
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

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
