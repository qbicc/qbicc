package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.ArrayType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An extracted element of an array value.
 */
public final class ExtractElement extends AbstractValue {
    private final Value arrayValue;
    private final ArrayType arrayType;
    private final Value index;

    ExtractElement(Node callSite, ExecutableElement element, int line, int bci, Value arrayValue, Value index) {
        super(callSite, element, line, bci);
        this.arrayValue = arrayValue;
        arrayType = (ArrayType) arrayValue.getType();
        this.index = index;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(arrayValue, index);
    }

    @Override
    String getNodeName() {
        return "ExtractElement";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ExtractElement && equals((ExtractElement) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        arrayValue.toReferenceString(b);
        b.append(',');
        index.toReferenceString(b);
        b.append(')');
        return b;
    }

    public boolean equals(ExtractElement other) {
        return this == other || other != null && arrayValue.equals(other.arrayValue) && index.equals(other.index);
    }

    public ArrayType getArrayType() {
        return arrayType;
    }

    public Value getArrayValue() {
        return arrayValue;
    }

    public Value getIndex() {
        return index;
    }

    @Override
    public ValueType getType() {
        return arrayType.getElementType();
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? arrayValue : index == 1 ? this.index : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(final ValueVisitorLong<T> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isConstant() {
        return arrayValue.isConstant() && index.isConstant();
    }
}
