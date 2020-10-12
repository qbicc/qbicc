package cc.quarkus.qcc.graph;

/**
 * A visitor over a graph of values.  Values form a directed acyclic graph (DAG).
 */
public interface ValueVisitor<T, R> {
    default R visitUnknown(final T param, Value node) {
        return null;
    }

    default R visit(T param, Add node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, And node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ArrayElementRead node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ArrayLength node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, BitCast node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Catch node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpEq node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpGe node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpGt node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpLe node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpLt node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, CmpNe node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ConstantValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ConstructorInvocation node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Convert node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Div node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Extend node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceFieldRead node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceInvocationValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceOf node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Mod node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Multiply node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Neg node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, New node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, NewArray node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Or node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ParameterValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, PhiValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Rol node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Ror node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Select node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Shl node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Shr node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, StaticFieldRead node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, StaticInvocationValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Sub node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ThisValue node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Truncate node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, Xor node) {
        return visitUnknown(param, node);
    }
}
