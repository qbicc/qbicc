package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;

/**
 * A node representing the byte offset of a field.
 */
public final class OffsetOfField extends AbstractValue {
    private final FieldElement fieldElement;
    private final SignedIntegerType type;

    OffsetOfField(final Node callSite, final ExecutableElement element, final int line, final int bci, FieldElement fieldElement, final SignedIntegerType type) {
        super(callSite, element, line, bci);
        this.fieldElement = fieldElement;
        this.type = type;
    }

    public FieldElement getFieldElement() {
        return fieldElement;
    }

    public SignedIntegerType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(OffsetOfField.class, fieldElement);
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    public boolean equals(final Object other) {
        return other instanceof OffsetOfField && equals((OffsetOfField) other);
    }

    public boolean equals(final OffsetOfField other) {
        return this == other || other != null
            && fieldElement.equals(other.fieldElement);
    }
}
