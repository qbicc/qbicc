package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface BinaryOp extends OwnedValue {
    Value getLeft();
    void setLeft(Value value);
    Value getRight();
    void setRight(Value value);
}
