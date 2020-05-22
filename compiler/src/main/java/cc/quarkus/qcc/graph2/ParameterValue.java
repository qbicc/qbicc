package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface ParameterValue extends OwnedValue {
    int getIndex();
    void setIndex(int idx);
    String getName();
    void setName(String name);
}
