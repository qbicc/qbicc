package org.qbicc.graph;

/**
 *
 */
public interface ValueHandleVisitor<T, R> {
    default R visitUnknown(T t, ValueHandle node) {
        return null;
    }

    default R visit(T t, ConstructorElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ExactMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, FunctionElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InterfaceMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, VirtualMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, StaticMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, UnsafeHandle node) {
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
        default R visit(T t, ConstructorElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, ExactMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, FunctionElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, InterfaceMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, VirtualMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, StaticMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, UnsafeHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, PointerHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

    }
}
