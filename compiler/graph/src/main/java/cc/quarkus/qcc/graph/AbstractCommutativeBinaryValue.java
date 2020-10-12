package cc.quarkus.qcc.graph;

/**
 *
 */
abstract class AbstractCommutativeBinaryValue extends AbstractBinaryValue implements CommutativeBinaryValue {
    AbstractCommutativeBinaryValue(Value left, Value right) {
        super(left, right);
    }
}
