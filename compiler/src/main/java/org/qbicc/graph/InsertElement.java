package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.ArrayType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An array value with an inserted element.
 */
public final class InsertElement extends AbstractValue implements Unschedulable {
    private final Value arrayValue;
    private final ArrayType arrayType;
    private final Value index;
    private final Value insertedValue;

    InsertElement(Node callSite, ExecutableElement element, int line, int bci, Value arrayValue, Value index, Value insertedValue) {
        super(callSite, element, line, bci);
        this.arrayValue = arrayValue;
        arrayType = (ArrayType) arrayValue.getType();
        this.index = index;
        this.insertedValue = insertedValue;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(arrayValue, index, insertedValue);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InsertElement && equals((InsertElement) other);
    }

    public boolean equals(InsertElement other) {
        return this == other || other != null && arrayValue.equals(other.arrayValue) && index.equals(other.index) && insertedValue.equals(other.insertedValue);
    }

    public Value getArrayValue() {
        return arrayValue;
    }

    public Value getIndex() {
        return index;
    }

    public Value getInsertedValue() {
        return insertedValue;
    }

    @Override
    public ArrayType getType() {
        return arrayType;
    }

    @Override
    public int getValueDependencyCount() {
        return 3;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? arrayValue : index == 1 ? this.index : index == 2 ? insertedValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
