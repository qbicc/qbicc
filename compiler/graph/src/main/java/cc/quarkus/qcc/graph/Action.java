package cc.quarkus.qcc.graph;

/**
 * A node which represents an action whose side-effects are not captured as a value.
 */
public interface Action extends Node {
    <T, R> R accept(ActionVisitor<T, R> visitor, T param);

    default <T, R> R accept(NodeVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
