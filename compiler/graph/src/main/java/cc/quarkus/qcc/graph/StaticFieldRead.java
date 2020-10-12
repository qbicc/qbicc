package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 *
 */
public final class StaticFieldRead extends AbstractValue implements FieldRead {
    private final Node dependency;
    private final FieldElement fieldElement;
    private final JavaAccessMode mode;

    StaticFieldRead(final GraphFactory.Context ctxt, final FieldElement fieldElement, final JavaAccessMode mode) {
        this.fieldElement = fieldElement;
        this.mode = mode;
        this.dependency = ctxt.getDependency();
        ctxt.setDependency(this);
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
