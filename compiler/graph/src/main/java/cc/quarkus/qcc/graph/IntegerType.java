package cc.quarkus.qcc.graph;

/**
 *
 */
public interface IntegerType extends NumericType {

    long and(long v1, long v2);
    long or(long v1, long v2);
    long xor(long v1, long v2);

    /**
     * Sign-extend the given value <em>of this type</em> to a full 64 bits.
     *
     * @param value the value of this type
     * @return the sign-extended value
     */
    long signExtend(long value);
    /**
     * Zero-extend the given value <em>of this type</em> to a full 64 bits.
     *
     * @param value the value of this type
     * @return the sign-extended value
     */
    long zeroExtend(long value);
}
