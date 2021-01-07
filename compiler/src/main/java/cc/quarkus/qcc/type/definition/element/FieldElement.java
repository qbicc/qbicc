package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.graph.literal.Literal;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;

/**
 *
 */
public final class FieldElement extends VariableElement implements MemberElement {
    public static final FieldElement[] NO_FIELDS = new FieldElement[0];
    private final Literal initialValue;

    FieldElement(Builder builder) {
        super(builder);
        this.initialValue = builder.initialValue;
    }

    public boolean isVolatile() {
        return hasAllModifiersOf(ClassFile.ACC_VOLATILE);
    }

    public Literal getInitialValue() {
        return initialValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static final class Builder extends VariableElement.Builder implements MemberElement.Builder {
        Builder() {}

        private Literal initialValue;

        public void setInitialValue(final Literal initialValue) {
            this.initialValue = initialValue;
        }

        public FieldElement build() {
            return new FieldElement(this);
        }
    }
}
