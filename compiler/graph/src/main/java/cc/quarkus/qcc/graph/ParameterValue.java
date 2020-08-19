package cc.quarkus.qcc.graph;

/**
 *
 */
public interface ParameterValue extends Value {
    int getIndex();
    void setIndex(int idx);

    void setType(Type type);
}
