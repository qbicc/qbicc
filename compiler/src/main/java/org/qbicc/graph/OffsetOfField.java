package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.definition.element.FieldElement;

/**
 * A node representing the byte offset of a field.
 */
public final class OffsetOfField extends AbstractValue {
    private final FieldElement fieldElement;
    private final SignedIntegerType type;

    OffsetOfField(final ProgramLocatable pl, FieldElement fieldElement, final SignedIntegerType type) {
        super(pl);
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
    String getNodeName() {
        return "OffsetOfField";
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

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('{');
        b.append(fieldElement);
        b.append('}');
        return b;
    }

    public boolean equals(final OffsetOfField other) {
        return this == other || other != null
            && fieldElement.equals(other.fieldElement);
    }
}
