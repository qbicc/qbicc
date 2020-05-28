package cc.quarkus.qcc.graph2;

/**
 * A terminator which designates a subsequent block for normal execution.
 */
public interface Goto extends Terminator {
    BasicBlock getNextBlock();
    void setNextBlock(BasicBlock branch);
}
