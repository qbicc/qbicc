package cc.quarkus.qcc.graph;

/**
 *
 */
abstract class AbstractBinaryValue extends AbstractValue implements BinaryValue {
    final Value left;
    final Value right;

    AbstractBinaryValue(final Value left, final Value right) {
        this.left = left;
        this.right = right;
    }

    public Value getLeftInput() {
        return left;
    }

    public Value getRightInput() {
        return right;
    }
}
