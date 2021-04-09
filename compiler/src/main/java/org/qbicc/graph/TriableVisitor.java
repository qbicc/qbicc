package org.qbicc.graph;

/**
 * A visitor over a graph of triable nodes.  Triable nodes can be actions or values, so classes which
 * implement this visitor type as well as one or more of those visitor types must take care to ensure that the type
 * arguments are all the same to avoid compilation errors.
 */
public interface TriableVisitor<T, R> {
    default R visitUnknown(T param, Triable node) {
        return null;
    }

    default R visit(T param, ConstructorInvocation node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, FunctionCall node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceInvocation node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceInvocationValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, StaticInvocation node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, StaticInvocationValue node) {
        return visitUnknown(param, node);
    }
}
