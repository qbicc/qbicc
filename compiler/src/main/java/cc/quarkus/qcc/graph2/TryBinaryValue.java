package cc.quarkus.qcc.graph2;

/**
 * A binary value that can result in a thrown exception (division or modulus) within a {@code try} block.
 */
public interface TryBinaryValue extends NonCommutativeBinaryValue, Try {

}
