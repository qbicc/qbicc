package org.qbicc.pointer;

import org.qbicc.interpreter.Memory;
import org.qbicc.type.ArrayType;
import org.qbicc.type.ValueType;

/**
 * A pointer to an array element.
 */
public final class ElementPointer extends Pointer {
    private final Pointer arrayPointer;
    private final long index;

    public ElementPointer(Pointer arrayPointer, long index) {
        super(((ArrayType)arrayPointer.getType().getPointeeType()).getElementType().getPointer());
        this.arrayPointer = arrayPointer;
        this.index = index;
    }

    public Pointer getArrayPointer() {
        return arrayPointer;
    }

    public long getIndex() {
        return index;
    }

    public ArrayType getPointerArrayType() {
        return (ArrayType) arrayPointer.getType().getPointeeType();
    }

    public ValueType getArrayElementType() {
        return getPointerArrayType().getElementType();
    }

    @Override
    public Pointer offsetByElements(long count) {
        if (count == 0) {
            return this;
        }
        return new ElementPointer(arrayPointer, index + count);
    }

    @Override
    public RootPointer getRootPointer() {
        return arrayPointer.getRootPointer();
    }

    @Override
    public long getRootByteOffset() {
        return arrayPointer.getRootByteOffset() + index * getArrayElementType().getSize();
    }

    @Override
    public Memory getRootMemoryIfExists() {
        return arrayPointer.getRootMemoryIfExists();
    }

    @Override
    public String getRootSymbolIfExists() {
        return arrayPointer.getRootSymbolIfExists();
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return arrayPointer.toString(b).append('[').append(index).append(']');
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
