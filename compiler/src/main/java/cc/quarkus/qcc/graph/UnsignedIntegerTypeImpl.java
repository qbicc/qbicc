package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
abstract class UnsignedIntegerTypeImpl implements UnsignedIntegerType {
    private final Constraint constraint;

    UnsignedIntegerTypeImpl(final int minValue, final int maxValue) {
        constraint = Constraint.greaterThanOrEqualTo(new ConstantValue32(minValue, this)).intersect(Constraint.lessThanOrEqualTo(new ConstantValue32(maxValue, this)));
    }

    UnsignedIntegerTypeImpl(final long minValue, final long maxValue) {
        constraint = Constraint.greaterThanOrEqualTo(new LongConstantValueImpl(minValue, this)).intersect(Constraint.lessThanOrEqualTo(new LongConstantValueImpl(maxValue, this)));
    }

    public ConstantValue bitCast(final ConstantValue other) {
        Type otherType = other.getConstantType();
        if (otherType instanceof WordType) {
            WordType otherWordType = (WordType) otherType;
            if (getSize() == otherWordType.getSize()) {
                return other.withTypeRaw(this);
            }
        }
        throw new IllegalArgumentException("Cannot bitcast from " + other + " to " + this);
    }

    public int getParameterCount() {
        return 1;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        if (index == 0) {
            return "value";
        } else {
            throw new IndexOutOfBoundsException(index);
        }
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        if (index == 0) {
            return constraint;
        } else {
            throw new IndexOutOfBoundsException(index);
        }
    }

    public void replaceWith(final Node node) {
        throw new UnsupportedOperationException("Cannot replace core word types");
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {

    }

    public int getIdForGraph() {
        return 0;
    }

    public void setIdForGraph(final int id) {

    }
}
