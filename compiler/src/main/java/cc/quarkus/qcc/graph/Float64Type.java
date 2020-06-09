package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class Float64Type implements FloatType {
    private final Constraint constraint;

    Float64Type() {
        constraint = Constraint.greaterThanOrEqualTo(new ConstantValue64(Double.doubleToLongBits(Double.MIN_VALUE), this)).union(Constraint.lessThanOrEqualTo(new ConstantValue64(Double.doubleToLongBits(Double.MAX_VALUE), this)));
    }

    public boolean isClass2Type() {
        return true;
    }

    public int getSize() {
        return 8;
    }

    public ConstantValue bitCast(final ConstantValue other) {
        Type otherType = other.getType();
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

    public String getLabelForGraph() {
        return "float64";
    }

    public int getIdForGraph() {
        return 0;
    }

    public void setIdForGraph(final int id) {

    }
}
