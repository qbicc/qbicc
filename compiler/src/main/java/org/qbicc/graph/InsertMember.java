package org.qbicc.graph;

import java.util.Objects;

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
    public boolean equals(Object other) {
        return other instanceof InsertMember && equals((InsertMember) other);
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
}
