package cc.quarkus.qcc.graph;

/**
 *
 */
public interface ParameterValue extends ProgramNode, Value {
    int getIndex();
    void setIndex(int idx);
    String getName();
    void setName(String name);
}
