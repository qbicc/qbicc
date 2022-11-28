package org.qbicc.graph;

/**
 *
 */
public interface ValueHandleVisitor<T, R> {
    default R visitUnknown(T t, ValueHandle node) {
        return null;
    }

    interface Delegating<T, R> extends ValueHandleVisitor<T, R> {
        ValueHandleVisitor<T, R> getDelegateValueHandleVisitor();

        @Override
        default R visitUnknown(T t, ValueHandle node) {
            return node.accept(getDelegateValueHandleVisitor(), t);
        }

    }
}
