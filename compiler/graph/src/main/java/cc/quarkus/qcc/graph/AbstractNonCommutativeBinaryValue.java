package cc.quarkus.qcc.graph;

/**
 *
 */
abstract class AbstractNonCommutativeBinaryValue extends AbstractBinaryValue implements NonCommutativeBinaryValue {
    AbstractNonCommutativeBinaryValue(Value left, Value right) {
        super(left, right);
    }
}
