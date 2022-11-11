package org.qbicc.graph;

/**
 *
 */
public interface ValueHandleVisitorLong<T> {
    default long visitUnknown(T t, ValueHandle node) {
        return 0;
    }

    default long visit(T t, AsmHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, ConstructorElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, CurrentThread node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, ElementOf node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, ExactMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, FunctionElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, GlobalVariable node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, InstanceFieldOf node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, InterfaceMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, VirtualMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, LocalVariable node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, MemberOf node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, StaticField node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, StaticMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default long visit(T t, UnsafeHandle node) {
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
        default long visit(T t, AsmHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, ConstructorElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, CurrentThread node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, ElementOf node) {
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
        default long visit(T t, GlobalVariable node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, InstanceFieldOf node) {
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
        default long visit(T t, LocalVariable node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, MemberOf node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, StaticField node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, StaticMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, UnsafeHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, PointerHandle node) {
            return getDelegateValueHandleVisitor().visit(t, node);
        }

    }
}
