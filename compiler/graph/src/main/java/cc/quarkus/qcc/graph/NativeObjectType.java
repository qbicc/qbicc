package cc.quarkus.qcc.graph;

/**
 * An "object" in memory, which consists of word types and structured types.
 */
public interface NativeObjectType extends Type {
    default boolean isComplete() {
        return true;
    }
}
