package cc.quarkus.qcc.graph;

/**
 *
 */
public interface Jsr extends Terminator {

    BasicBlock getTarget();

    BasicBlock getReturn();

    Value getReturnAddressValue();
}
