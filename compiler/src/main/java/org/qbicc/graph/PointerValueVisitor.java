package org.qbicc.graph;

/**
 *
 */
public interface PointerValueVisitor<T, R> {
    default R visitUnknown(T t, PointerValue node) {
        return null;
    }

    default R visit(T t, AsmHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ConstructorElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, CurrentThread node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ElementOf node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, ExactMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, FunctionElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, GlobalVariable node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InstanceFieldOf node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, InterfaceMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, VirtualMethodElementHandle node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, LocalVariable node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, MemberOf node) {
        return visitUnknown(t, node);
    }

    default R visit(T t, StaticField node) {
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

    default R visit(T t, ReferenceHandle node) {
        return visitUnknown(t, node);
    }

    interface Delegating<T, R> extends PointerValueVisitor<T, R> {
        PointerValueVisitor<T, R> getDelegateValueVisitor();

        @Override
        default R visitUnknown(T t, PointerValue node) {
            return node.accept(getDelegateValueVisitor(), t);
        }

        @Override
        default R visit(T t, AsmHandle node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, ConstructorElementHandle node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, CurrentThread node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, ElementOf node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, ExactMethodElementHandle node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, FunctionElementHandle node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, GlobalVariable node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, InstanceFieldOf node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, InterfaceMethodElementHandle node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, VirtualMethodElementHandle node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, LocalVariable node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, MemberOf node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, StaticField node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, StaticMethodElementHandle node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, UnsafeHandle node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, PointerHandle node) {
            return getDelegateValueVisitor().visit(t, node);
        }

        @Override
        default R visit(T t, ReferenceHandle node) {
            return getDelegateValueVisitor().visit(t, node);
        }
    }
}
