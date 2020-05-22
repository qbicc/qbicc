package cc.quarkus.qcc.graph2;

/**
 * A value which is owned (produced) by a basic block.  Uses of the value must be generated in the source block.
 */
public interface OwnedValue extends Value {
    BasicBlock getOwner();
    void setOwner(BasicBlock owner);
}
