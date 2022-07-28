package org.qbicc.graph;

/**
 * A visitor over a graph of terminator nodes.  Terminator nodes form a directed graph which may contain cycles.
 */
public interface TerminatorVisitor<T, R> {
    default R visitUnknown(T t, Terminator node) {
        return null;
    }

    default R visit(T t, CallNoReturn node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Goto node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, If node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Invoke node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InvokeNoReturn node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Jsr node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Ret node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Return node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Switch node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, TailCall node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, TailInvoke node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Throw node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, TypeSwitch node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ValueReturn node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, Unreachable node) { return visitUnknown(t, node); }

    // Errors

    interface Delegating<T, R> extends TerminatorVisitor<T, R> {
        TerminatorVisitor<T, R> getDelegateTerminatorVisitor();

        default R visitUnknown(T t, Terminator node) {
            return node.accept(getDelegateTerminatorVisitor(), t);
        }

        default R visit(T t, CallNoReturn node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, Goto node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, If node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, Invoke node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, InvokeNoReturn node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, Jsr node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, Ret node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, Return node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, Unreachable node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, Switch node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, TailCall node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, TailInvoke node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, Throw node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, TypeSwitch node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

        default R visit(T t, ValueReturn node) {
            return getDelegateTerminatorVisitor().visit(t, node);
        }

    }
}
