package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.StructType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An extracted member of a compound (structure) value.
 */
public final class ExtractMember extends AbstractValue {
    private final Value structValue;
    private final StructType structType;
    private final StructType.Member member;

    ExtractMember(Node callSite, ExecutableElement element, int line, int bci, Value structValue, StructType.Member member) {
        super(callSite, element, line, bci);
        this.structValue = structValue;
        structType = (StructType) structValue.getType();
        this.member = member;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(structValue, member);
    }

    @Override
    String getNodeName() {
        return "ExtractMember";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ExtractMember && equals((ExtractMember) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        structValue.toReferenceString(b);
        b.append(',');
        member.toString(b);
        b.append(')');
        return b;
    }

    public boolean equals(ExtractMember other) {
        return this == other || other != null && structValue.equals(other.structValue) && member.equals(other.member);
    }

    public StructType getStructType() {
        return structType;
    }

    public Value getStructValue() {
        return structValue;
    }

    @Override
    public ValueType getType() {
        return member.getType();
    }

    public StructType.Member getMember() {
        return member;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? structValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isConstant() {
        return structValue.isConstant();
    }
}
