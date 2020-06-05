package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

import cc.quarkus.qcc.constraint.Constraint;

/**
 *
 */
final class Float32Type implements FloatType {
    private final Constraint constraint;

    Float32Type() {
        constraint = Constraint.greaterThanOrEqualTo(new ConstantValue32(Float.floatToIntBits(Float.MIN_VALUE), this)).union(Constraint.lessThanOrEqualTo(new ConstantValue32(Float.floatToIntBits(Float.MAX_VALUE), this)));
    }

    public int getSize() {
        return 4;
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
        return "float32";
    }

    public int getIdForGraph() {
        return 0;
    }

    public void setIdForGraph(final int id) {

    }
}
