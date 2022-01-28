package org.qbicc.graph;

/**
 * A visitor over a graph of non-value action nodes.  Non-value action nodes form a directed acyclic graph (DAG).
 */
public interface ActionVisitor<T, R> {
    default R visitUnknown(T param, Action node) {
        return null;
    }

    default R visit(T param, BlockEntry node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InitCheck node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, DebugAddressDeclaration node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, DebugValueDeclaration node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Fence node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MonitorEnter node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MonitorExit node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Store node) {
        return visitUnknown(param, node);
    }

    interface Delegating<T, R> extends ActionVisitor<T, R> {
        ActionVisitor<T, R> getDelegateActionVisitor();

        default R visitUnknown(T param, Action node) {
            return node.accept(getDelegateActionVisitor(), param);
        }

        default R visit(T param, BlockEntry node) {
            return getDelegateActionVisitor().visit(param, node);
        }

        default R visit(T param, InitCheck node) {
            return getDelegateActionVisitor().visit(param, node);
        }

        default R visit(T param, DebugAddressDeclaration node) {
            return getDelegateActionVisitor().visit(param, node);
        }

        default R visit(T param, DebugValueDeclaration node) {
            return getDelegateActionVisitor().visit(param, node);
        }

        default R visit(T param, Fence node) {
            return getDelegateActionVisitor().visit(param, node);
        }

        default R visit(T param, MonitorEnter node) {
            return getDelegateActionVisitor().visit(param, node);
        }

        default R visit(T param, MonitorExit node) {
            return getDelegateActionVisitor().visit(param, node);
        }

        default R visit(T param, Store node) {
            return getDelegateActionVisitor().visit(param, node);
        }
    }
}
