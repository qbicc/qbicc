package cc.quarkus.qcc.graph2;

/**
 * A value which is owned (produced) intrinsically by a basic block.  Uses of the value must be generated in the source block.
 */
public interface ProgramNode extends Node {
    BasicBlock getOwner();
    void setOwner(BasicBlock owner);
    int getSourceLine();
    void setSourceLine(int line);
}
