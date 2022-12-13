package org.qbicc.graph.literal;

import org.qbicc.graph.Value;
import org.qbicc.type.ArrayType;
import org.qbicc.type.PointerType;
import org.qbicc.type.WordType;

public class ElementOfLiteral extends Literal {
    final Literal arrayPointer;
    final Literal index;

    ElementOfLiteral(Literal arrayPointer, Literal index) {
        this.arrayPointer = arrayPointer;
        this.index = index;
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return switch (index) {
            case 0 -> arrayPointer;
            case 1 -> this.index;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public boolean equals(Literal other) {
        return other instanceof ElementOfLiteral && equals((ElementOfLiteral) other);
    }

    public boolean equals(ElementOfLiteral other) {
        return other == this || other != null && arrayPointer.equals(other.arrayPointer) && index.equals(other.index);
    }

    @Override
    public int hashCode() { return arrayPointer.hashCode() * 19 + index.hashCode(); }

    @Override
    public PointerType getType() {
        return arrayPointer.getPointeeType(ArrayType.class).getElementType().getPointer();
    }

    public Literal getArrayPointer() { return arrayPointer; }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public Literal getIndex() {
        return index;
    }

    @Override
    Literal bitCast(LiteralFactory lf, WordType toType) {
        if (toType.equals(arrayPointer.getType()) && index.isZero()) {
            // cast element zero to the array type = get the array pointer
            return arrayPointer;
        }
        return super.bitCast(lf, toType);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append("element_of").append('(');
        arrayPointer.toString(b);
        b.append(',');
        index.toString(b);
        b.append(')');
        return b;
    }
}
