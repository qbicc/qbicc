package cc.quarkus.qcc.graph;

import java.io.IOException;
import java.util.Set;

import cc.quarkus.qcc.constraint.Constraint;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class BooleanTypeImpl implements BooleanType {
    final ConstantValue false_ = new ConstantValue32(0, this);
    final ConstantValue true_ = new ConstantValue32(1, this);
    private final Constraint constraint = Constraint.greaterThanOrEqualTo(false_).intersect(Constraint.lessThanOrEqualTo(true_));

    public int getSize() {
        return 1;
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
        return 0;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public void replaceWith(final Node node) {
        throw Assert.unsupported();
    }

    public void writeToGraph(final Set<Node> visited, final Appendable graph, final Set<BasicBlock> knownBlocks) throws IOException {

    }

    public String getLabelForGraph() {
        return "boolean";
    }

    public int getIdForGraph() {
        return 0;
    }

    public void setIdForGraph(final int id) {

    }
}
