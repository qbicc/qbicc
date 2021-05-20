package org.qbicc.graph;

/**
 * A visitor over a graph of terminator nodes.  Terminator nodes form a directed graph which may contain cycles.
 */
public interface TerminatorVisitor<T, R> {
    default R visitUnknown(T param, Terminator node) {
        return null;
    }

    default R visit(T param, CallNoReturn node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Goto node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, If node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Invoke node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InvokeNoReturn node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Jsr node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Ret node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Return node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Switch node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, TailCall node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, TailInvoke node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Throw node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ValueReturn node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Unreachable node) { return visitUnknown(param, node); }

    // Errors

    default R visit(T param, ClassCastErrorNode node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, NoSuchMethodErrorNode node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ClassNotFoundErrorNode node) {
        return visitUnknown(param, node);
    }

    interface Delegating<T, R> extends TerminatorVisitor<T, R> {
        TerminatorVisitor<T, R> getDelegateTerminatorVisitor();

        default R visitUnknown(T param, Terminator node) {
            return node.accept(getDelegateTerminatorVisitor(), param);
        }

        default R visit(T param, CallNoReturn node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Goto node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, If node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Invoke node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, InvokeNoReturn node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Jsr node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Ret node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Return node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Unreachable node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Switch node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, TailCall node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, TailInvoke node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, Throw node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, ValueReturn node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, ClassCastErrorNode node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, NoSuchMethodErrorNode node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }

        default R visit(T param, ClassNotFoundErrorNode node) {
            return getDelegateTerminatorVisitor().visit(param, node);
        }
    }
}
