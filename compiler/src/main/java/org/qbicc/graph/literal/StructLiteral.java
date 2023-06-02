package org.qbicc.graph.literal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.qbicc.graph.Value;
import org.qbicc.type.StructType;

/**
 *
 */
public final class StructLiteral extends Literal {

    private final StructType type;
    private final Map<StructType.Member, Literal> values;
    private final List<Literal> valuesAsList;
    private final int hashCode;

    StructLiteral(final StructType type, final Map<StructType.Member, Literal> values) {
        this.type = type;
        this.values = values;
        hashCode = Objects.hash(type, values);
        valuesAsList = new ArrayList<>(values.size());
        for (StructType.Member member : type.getMembers()) {
            Literal literal = values.get(member);
            if (literal != null) {
                valuesAsList.add(literal);
            }
        }
    }

    @Override
    public int getValueDependencyCount() {
        return valuesAsList.size();
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return valuesAsList.get(index);
    }

    public StructType getType() {
        return type;
    }

    public Map<StructType.Member, Literal> getValues() {
        return values;
    }

    public <T, R> R accept(final LiteralVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public Value extractMember(LiteralFactory lf, StructType.Member member) {
        return values.get(member);
    }

    public boolean isZero() {
        return false;
    }

    public boolean equals(final Literal other) {
        return other instanceof StructLiteral && equals((StructLiteral) other);
    }

    public boolean equals(final StructLiteral other) {
        return this == other || other != null && hashCode == other.hashCode && type.equals(other.type) && values.equals(other.values);
    }

    public int hashCode() {
        return hashCode;
    }

    public StringBuilder toString(StringBuilder builder) {
        builder.append('{');
        Iterator<Map.Entry<StructType.Member, Literal>> iterator = values.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<StructType.Member, Literal> entry = iterator.next();
            builder.append(entry.getKey().getName());
            builder.append('=');
            builder.append(entry.getValue());
            while (iterator.hasNext()) {
                builder.append(',');
                entry = iterator.next();
                builder.append(entry.getKey().getName());
                builder.append('=');
                builder.append(entry.getValue());
            }
        }
        builder.append('}');
        return builder;
    }
}
