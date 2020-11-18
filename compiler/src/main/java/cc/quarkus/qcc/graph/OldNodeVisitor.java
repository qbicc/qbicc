package cc.quarkus.qcc.graph;

/**
 * A general visitor for the different node types.
 */
@Deprecated
public interface OldNodeVisitor<T, R> {
    default R visitUnknown(T param, Node node) {
        return null;
    }

    default R visit(T param, Action node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Terminator node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Value node) {
        return visitUnknown(param, node);
    }

    @Deprecated
    interface Delegating<T, R> extends OldNodeVisitor<T, R> {
        OldNodeVisitor<T, R> getDelegateNodeVisitor();

        default R visitUnknown(T param, Node node) {
            return node.accept(getDelegateNodeVisitor(), param);
        }

        default R visit(T param, Action node) {
            return getDelegateNodeVisitor().visit(param, node);
        }

        default R visit(T param, Terminator node) {
            return getDelegateNodeVisitor().visit(param, node);
        }

        default R visit(T param, Value node) {
            return getDelegateNodeVisitor().visit(param, node);
        }
    }
}
