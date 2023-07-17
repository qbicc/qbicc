package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.FieldElement;

/**
 * An extracted field value of an object value (not a reference).
 */
public final class ExtractInstanceField extends AbstractValue {
    private final Value objectValue;
    private final PhysicalObjectType objectType;
    private final FieldElement fieldElement;
    private final ValueType valueType;

    ExtractInstanceField(final ProgramLocatable pl, Value objectValue, FieldElement fieldElement, ValueType valueType) {
        super(pl);
        this.objectValue = objectValue;
        objectType = (PhysicalObjectType) objectValue.getType();
        this.fieldElement = fieldElement;
        this.valueType = valueType;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(objectValue, fieldElement);
    }

    @Override
    String getNodeName() {
        return "ExtractInstanceField";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ExtractInstanceField && equals((ExtractInstanceField) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        objectValue.toReferenceString(b);
        b.append(',');
        // todo: fieldElement.toString(b)
        b.append(fieldElement);
        b.append(')');
        return b;
    }

    public boolean equals(ExtractInstanceField other) {
        return this == other || other != null && objectValue.equals(other.objectValue) && fieldElement.equals(other.fieldElement);
    }

    public PhysicalObjectType getObjectType() {
        return objectType;
    }

    public Value getObjectValue() {
        return objectValue;
    }

    @Override
    public ValueType getType() {
        return valueType;
    }

    public FieldElement getFieldElement() {
        return fieldElement;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? objectValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
