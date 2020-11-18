package cc.quarkus.qcc.graph;

/**
 *
 */
abstract class AbstractBinaryValue extends AbstractValue implements BinaryValue {
    final Value left;
    final Value right;

    AbstractBinaryValue(final int line, final int bci, final Value left, final Value right) {
        super(line, bci);
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
