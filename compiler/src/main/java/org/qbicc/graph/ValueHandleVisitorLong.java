package org.qbicc.graph;

/**
 *
 */
public interface ValueHandleVisitorLong<T> {
    default long visitUnknown(T t, ValueHandle node) {
        return 0;
    }

    interface Delegating<T> extends ValueHandleVisitorLong<T> {
        ValueHandleVisitorLong<T> getDelegateValueHandleVisitor();

        @Override
        default long visitUnknown(T t, ValueHandle node) {
            return node.accept(getDelegateValueHandleVisitor(), t);
        }

    }
}
