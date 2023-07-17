package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.StructType;

/**
 * A compound (structure) value with an inserted member.
 */
public final class InsertMember extends AbstractValue {
    private final Value structValue;
    private final Value insertedValue;
    private final StructType structType;
    private final StructType.Member member;

    InsertMember(final ProgramLocatable pl, Value compoundValue, Value insertedValue, StructType.Member member) {
        super(pl);
        this.structValue = compoundValue;
        this.insertedValue = insertedValue;
        structType = (StructType) compoundValue.getType();
        this.member = member;
    }

    @Override
    int calcHashCode() {
        return Objects.hash(structValue, insertedValue, member);
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
        structValue.toReferenceString(b);
        b.append(',');
        member.toString(b);
        b.append(',');
        insertedValue.toReferenceString(b);
        b.append(')');
        return b;
    }

    public boolean equals(InsertMember other) {
        return this == other || other != null && structValue.equals(other.structValue) && insertedValue.equals(other.insertedValue) && member.equals(other.member);
    }

    public Value getStructValue() {
        return structValue;
    }

    public Value getInsertedValue() {
        return insertedValue;
    }

    @Override
    public StructType getType() {
        return structType;
    }

    public StructType.Member getMember() {
        return member;
    }

    @Override
    public int getValueDependencyCount() {
        return 2;
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? structValue : index == 1 ? insertedValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public Value extractMember(LiteralFactory lf, StructType.Member member) {
        return member.equals(this.member) ? insertedValue : structValue.extractMember(lf, member);
    }

    public boolean isConstant() {
        return structValue.isConstant() && insertedValue.isConstant();
    }
}
