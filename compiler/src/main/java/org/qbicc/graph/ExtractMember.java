package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.CompoundType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An extracted member of a compound (structure) value.
 */
public final class ExtractMember extends AbstractValue {
    private final Value compoundValue;
    private final CompoundType compoundType;
    private final CompoundType.Member member;

    ExtractMember(Node callSite, ExecutableElement element, int line, int bci, Value compoundValue, CompoundType.Member member) {
        super(callSite, element, line, bci);
        this.compoundValue = compoundValue;
        compoundType = (CompoundType) compoundValue.getType();
        this.member = member;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(compoundValue, member);
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
        compoundValue.toReferenceString(b);
        b.append(',');
        member.toString(b);
        b.append(')');
        return b;
    }

    public boolean equals(ExtractMember other) {
        return this == other || other != null && compoundValue.equals(other.compoundValue) && member.equals(other.member);
    }

    public CompoundType getCompoundType() {
        return compoundType;
    }

    public Value getCompoundValue() {
        return compoundValue;
    }

    @Override
    public ValueType getType() {
        return member.getType();
    }

    public CompoundType.Member getMember() {
        return member;
    }

    @Override
    public int getValueDependencyCount() {
        return 1;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? compoundValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isConstant() {
        return compoundValue.isConstant();
    }
}
