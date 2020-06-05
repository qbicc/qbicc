package cc.quarkus.qcc.graph;

/**
 *
 */
public interface NumericType extends ComparableWordType {
    int add(int v1, int v2);
    long add(long v1, long v2);

    int subtract(int v1, int v2);
    long subtract(long v1, long v2);

    int multiply(int v1, int v2);
    long multiply(long v1, long v2);

    int divide(int v1, int v2) throws ArithmeticException;
    long divide(long v1, long v2) throws ArithmeticException;

    int modulus(int v1, int v2) throws ArithmeticException;
    long modulus(long v1, long v2) throws ArithmeticException;
}
