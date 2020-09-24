package cc.quarkus.qcc.graph;

/**
 * A terminator which designates a subsequent block for normal execution.
 */
public interface Goto extends Terminator {
    BasicBlock getTarget();
    void setTarget(BasicBlock branch);
}
