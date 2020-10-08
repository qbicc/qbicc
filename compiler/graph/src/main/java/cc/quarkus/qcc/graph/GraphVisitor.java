package cc.quarkus.qcc.graph;

/**
 * A visitor over the program nodes within a basic block.
 */
public interface GraphVisitor<P> {
    default void visitDependencies(P param, Node node) {
        int cnt = node.getBasicDependencyCount();
        for (int i = 0; i < cnt; i++) {
            visitDependency(param, node.getBasicDependency(i));
        }
        cnt = node.getValueDependencyCount();
        for (int i = 0; i < cnt; i++) {
            visitDependency(param, node.getValueDependency(i));
        }
    }

    default void visitDependency(P param, Node dependency) {
        dependency.accept(this, param);
    }

    default void visitUnknown(P param, Node node) {
    }

    default void visit(P param, ArrayElementReadValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, ArrayElementWrite node) {
        visitUnknown(param, node);
    }

    default void visit(P param, ArrayLengthValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, CatchValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, CommutativeBinaryValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, ConstantValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, FieldReadValue node) {
        // note: static field reads only
        visitUnknown(param, node);
    }

    default void visit(P param, FieldWrite node) {
        // note: static field writes only
        visitUnknown(param, node);
    }

    default void visit(P param, Goto node) {
        visitUnknown(param, node);
    }

    default void visit(P param, If node) {
        visitUnknown(param, node);
    }

    default void visit(P param, IfValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, InstanceFieldReadValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, InstanceFieldWrite node) {
        visitUnknown(param, node);
    }

    default void visit(P param, InstanceInvocation node) {
        visitUnknown(param, node);
    }

    default void visit(P param, InstanceInvocationValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, InstanceOfValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, Invocation node) {
        visitUnknown(param, node);
    }

    default void visit(P param, InvocationValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, Jsr node) {
        visitUnknown(param, node);
    }

    default void visit(P param, NewArrayValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, NewValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, NonCommutativeBinaryValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, ParameterValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, PhiValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, Ret node) {
        visitUnknown(param, node);
    }

    default void visit(P param, Return node) {
        visitUnknown(param, node);
    }

    default void visit(P param, Switch node) {
        visitUnknown(param, node);
    }

    default void visit(P param, ThisValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, Throw node) {
        visitUnknown(param, node);
    }

    default void visit(P param, TryInstanceInvocation node) {
        visitUnknown(param, node);
    }

    default void visit(P param, TryInstanceInvocationValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, TryInvocation node) {
        visitUnknown(param, node);
    }

    default void visit(P param, TryInvocationValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, TryThrow node) {
        visitUnknown(param, node);
    }

    default void visit(P param, UnaryValue node) {
        visitUnknown(param, node);
    }

    default void visit(P param, ValueReturn node) {
        visitUnknown(param, node);
    }

    default void visit(P param, WordCastValue node) {
        visitUnknown(param, node);
    }
}
