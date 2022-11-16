package org.qbicc.graph;

/**
 *
 */
public interface ValueHandleVisitorLong<T> {
    default long visitUnknown(T t, ValueHandle node) {
        return 0;
    }

    default long visit(T t, ConstructorElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, ExactMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, FunctionElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, InterfaceMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, VirtualMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, StaticMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, PointerHandle node) {
        return visitUnknown(t, node);
    }

    interface Delegating<T> extends ValueHandleVisitorLong<T> {
        ValueHandleVisitorLong<T> getDelegateValueHandleVisitor();

        @Override
        default long visitUnknown(T t, ValueHandle node) {
            return node.accept(getDelegateValueHandleVisitor(), t);
        }

        @Override
        default long visit(T t, ConstructorElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, ExactMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, FunctionElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, InterfaceMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, VirtualMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, StaticMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, PointerHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

    }
}
