package org.qbicc.graph;

/**
 * A visitor over a graph of non-value action nodes.  Non-value action nodes form a directed acyclic graph (DAG).
 */
public interface ActionVisitor<T, R> {
    default R visitUnknown(T t, Action node) {
        return null;
    }

    default R visit(T t, BlockEntry node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InitCheck node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InitializeClass node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, DebugAddressDeclaration node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, DebugValueDeclaration node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, EnterSafePoint node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ExitSafePoint node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Fence node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, MonitorEnter node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, MonitorExit node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Reachable node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, PollSafePoint node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Store node) {
        return visitUnknown(t, node);
    }

    interface Delegating<T, R> extends ActionVisitor<T, R> {
        ActionVisitor<T, R> getDelegateActionVisitor();

        default R visitUnknown(T t, Action node) {
            return node.accept(getDelegateActionVisitor(), t);
        }

        default R visit(T t, BlockEntry node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, InitCheck node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, InitializeClass node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, DebugAddressDeclaration node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, DebugValueDeclaration node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, EnterSafePoint node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, ExitSafePoint node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, Fence node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, MonitorEnter node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, MonitorExit node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, Reachable node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, PollSafePoint node) {
            return getDelegateActionVisitor().visit(t, node);
        }

        default R visit(T t, Store node) {
            return getDelegateActionVisitor().visit(t, node);
        }
    }
}
