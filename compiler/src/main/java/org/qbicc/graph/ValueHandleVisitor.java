package org.qbicc.graph;

/**
 *
 */
public interface ValueHandleVisitor<T, R> {
    default R visitUnknown(T t, ValueHandle node) {
        return null;
    }

    default R visit(T t, Executable node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, PointerHandle node) {
        return visitUnknown(t, node);
    }

    interface Delegating<T, R> extends ValueHandleVisitor<T, R> {
        ValueHandleVisitor<T, R> getDelegateValueHandleVisitor();

        @Override
        default R visitUnknown(T t, ValueHandle node) {
            return node.accept(getDelegateValueHandleVisitor(), t);
        }

        @Override
        default R visit(T t, Executable node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, PointerHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

    }
}
