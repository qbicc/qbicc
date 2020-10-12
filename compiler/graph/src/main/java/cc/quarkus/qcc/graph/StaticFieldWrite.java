package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 *
 */
public final class StaticFieldWrite extends AbstractNode implements FieldWrite, Action {
    private final Node dependency;
    private final FieldElement fieldElement;
    private final Value value;
    private final JavaAccessMode mode;

    StaticFieldWrite(final GraphFactory.Context ctxt, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        this.fieldElement = fieldElement;
        this.value = value;
        this.mode = mode;
        this.dependency = ctxt.getDependency();
        ctxt.setDependency(this);
    }

    public FieldElement getFieldElement() {
        return fieldElement;
    }

    public Value getWriteValue() {
        return value;
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

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
