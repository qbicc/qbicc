package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface ParameterValue extends ProgramNode, Value {
    int getIndex();
    void setIndex(int idx);
    String getName();
    void setName(String name);
}
