package cc.quarkus.qcc.graph;

/**
 *
 */
public final class Mod extends AbstractBinaryValue implements NonCommutativeBinaryValue {
    Mod(final int line, final int bci, final Value v1, final Value v2) {
        super(line, bci, v1, v2);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
