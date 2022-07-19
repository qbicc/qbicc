package org.qbicc.graph;

/**
 *
 */
public interface PointerValueVisitorLong<T> {
    default long visitUnknown(T t, PointerValue node) {
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

    default long visit(T t, ReferenceHandle node) {
        return visitUnknown(t, node);
    }

    interface Delegating<T> extends PointerValueVisitorLong<T> {
        PointerValueVisitorLong<T> getDelegatePointerHandleVisitor();

        @Override
        default long visitUnknown(T t, PointerValue node) {
            return node.accept(getDelegatePointerHandleVisitor(), t);
        }

        @Override
        default long visit(T t, AsmHandle node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, ConstructorElementHandle node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, CurrentThread node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, ElementOf node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, ExactMethodElementHandle node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, FunctionElementHandle node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, GlobalVariable node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, InstanceFieldOf node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, InterfaceMethodElementHandle node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, VirtualMethodElementHandle node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, LocalVariable node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, MemberOf node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, StaticField node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, StaticMethodElementHandle node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, UnsafeHandle node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, PointerHandle node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }

        @Override
        default long visit(T t, ReferenceHandle node) {
            return getDelegatePointerHandleVisitor().visit(t, node);
        }
    }
}
