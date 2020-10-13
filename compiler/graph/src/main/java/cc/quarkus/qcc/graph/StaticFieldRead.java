package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 *
 */
public final class StaticFieldRead extends AbstractValue implements FieldRead {
    private final Node dependency;
    private final FieldElement fieldElement;
    private final JavaAccessMode mode;

    StaticFieldRead(final Node dependency, final FieldElement fieldElement, final JavaAccessMode mode) {
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

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
