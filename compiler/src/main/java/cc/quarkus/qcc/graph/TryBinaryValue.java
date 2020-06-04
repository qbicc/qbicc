package cc.quarkus.qcc.graph;

/**
 * A binary value that can result in a thrown exception (division or modulus) within a {@code try} block.
 */
public interface TryBinaryValue extends NonCommutativeBinaryValue, Try {

}
