package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface Instruction extends Node {
    int getLine();
    boolean hasDependency();
    Instruction getDependency();
    void setDependency(Instruction dependency);
}
