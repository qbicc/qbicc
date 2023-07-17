package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;

/**
 * A pointer to an array element.
 */
public final class ElementOf extends AbstractValue {
    private final Value arrayPointer;
    private final Value index;
    private final PointerType pointerType;

    ElementOf(final ProgramLocatable pl, Value arrayPointer, Value index) {
        super(pl);
        this.arrayPointer = arrayPointer;
        this.index = index;

        ValueType inputType = arrayPointer.getPointeeType();
        PointerType pointerType;

        if (inputType instanceof ArrayType) {
            pointerType = ((ArrayType) inputType).getElementType().getPointer();
        } else if (inputType instanceof ArrayObjectType) {
            pointerType = ((ArrayObjectType) inputType).getElementType().getPointer();
        } else {
            throw new IllegalArgumentException("Invalid input type: " + inputType);
        }

        this.pointerType = pointerType.withQualifiersFrom(arrayPointer.getType(PointerType.class));
    }

    @Override
    public PointerType getType() {
        return pointerType;
    }

    @Override
    public boolean isConstant() {
        return index.isConstant() && arrayPointer.isConstant();
    }

    @Override
    public boolean isPointeeConstant() {
        return index.isConstant() && arrayPointer.isPointeeConstant();
    }

    public Value getArrayPointer() {
        return arrayPointer;
    }

    public Value getIndex() {
        return index;
    }

    @Override
    public AccessMode getDetectedMode() {
        return arrayPointer.getDetectedMode();
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int idx) throws IndexOutOfBoundsException {
        return switch (idx) {
            case 0 -> arrayPointer;
            case 1 -> index;
            default -> throw new IndexOutOfBoundsException(idx);
        };
    }

    int calcHashCode() {
        return Objects.hash(arrayPointer, index);
    }

    @Override
    String getNodeName() {
        return "ElementOf";
    }

    public boolean equals(final Object other) {
        return other instanceof ElementOf && equals((ElementOf) other);
    }

    @Override
    StringBuilder toRValueString(StringBuilder b) {
        b.append("element pointer ");
        arrayPointer.toReferenceString(b);
        b.append('[');
        index.toString(b);
        b.append(']');
        return b;
    }

    public boolean equals(final ElementOf other) {
        return this == other || other != null && arrayPointer.equals(other.arrayPointer) && index.equals(other.index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
