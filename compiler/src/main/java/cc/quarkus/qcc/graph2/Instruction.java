package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface Instruction extends Node {
    int getLine();

    Instruction getDependency();
    void setDependency(Instruction dependency);
}
