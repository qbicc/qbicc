package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.ReferenceType;

/**
 * The class object for a given type ID value.
 */
public final class ClassOf extends AbstractValue implements UnaryValue {
    private final Value input;
    private final Value dimensions;
    private final ReferenceType type;

    ClassOf(final ProgramLocatable pl, final Value input, Value dimensions, final ReferenceType type) {
        super(pl);
        this.input = input;
        this.dimensions = dimensions;
        this.type = type;
    }

    public Value getInput() {
        return input;
    }

    public Value getDimensions() { return dimensions; }

    public ReferenceType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(ClassOf.class, input, dimensions);
    }

    @Override
    String getNodeName() {
        return "ClassOf";
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    public boolean equals(final Object other) {
        return other instanceof ClassOf && equals((ClassOf) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        input.toReferenceString(b);
        b.append(',');
        dimensions.toReferenceString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final ClassOf other) {
        return this == other || other != null
            && input.equals(other.input)
            && dimensions.equals(other.dimensions);
    }
}
