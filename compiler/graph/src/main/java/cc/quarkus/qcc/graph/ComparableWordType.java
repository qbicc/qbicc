package cc.quarkus.qcc.graph;

/**
 *
 */
public interface ComparableWordType extends WordType {

    boolean isNegative(long value);
    boolean isNegative(int value);

    boolean isNotNegative(long value);
    boolean isNotNegative(int value);

    boolean isPositive(long value);
    boolean isPositive(int value);

    boolean isNotPositive(long value);
    boolean isNotPositive(int value);

    int compare(long val1, long val2);
    int compare(int val1, int val2);
}
