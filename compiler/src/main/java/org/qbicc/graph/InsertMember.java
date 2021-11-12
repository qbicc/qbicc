package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.CompoundType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A compound (structure) value with an inserted member.
 */
public final class InsertMember extends AbstractValue implements Unschedulable {
    private final Value compoundValue;
    private final Value insertedValue;
    private final CompoundType compoundType;
    private final CompoundType.Member member;

    InsertMember(Node callSite, ExecutableElement element, int line, int bci, Value compoundValue, Value insertedValue, CompoundType.Member member) {
        super(callSite, element, line, bci);
        this.compoundValue = compoundValue;
        this.insertedValue = insertedValue;
        compoundType = (CompoundType) compoundValue.getType();
        this.member = member;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(compoundValue, insertedValue, member);
    }

    @Override
    String getNodeName() {
        return "InsertMember";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InsertMember && equals((InsertMember) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        compoundValue.toString(b);
        b.append(',');
        member.toString(b);
        b.append(',');
        insertedValue.toString(b);
        b.append(')');
        return b;
    }

    public boolean equals(InsertMember other) {
        return this == other || other != null && compoundValue.equals(other.compoundValue) && insertedValue.equals(other.insertedValue) && member.equals(other.member);
    }

    public Value getCompoundValue() {
        return compoundValue;
    }

    public Value getInsertedValue() {
        return insertedValue;
    }

    @Override
    public CompoundType getType() {
        return compoundType;
    }

    public CompoundType.Member getMember() {
        return member;
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? compoundValue : index == 1 ? insertedValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public Value extractMember(LiteralFactory lf, CompoundType.Member member) {
        return member.equals(this.member) ? insertedValue : compoundValue.extractMember(lf, member);
    }

    public boolean isConstant() {
        return compoundValue.isConstant() && insertedValue.isConstant();
    }
}
