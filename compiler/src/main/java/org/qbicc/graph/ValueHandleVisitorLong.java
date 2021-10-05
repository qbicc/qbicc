package org.qbicc.graph;

/**
 *
 */
public interface ValueHandleVisitorLong<T> {
    default long visitUnknown(T param, ValueHandle node) {
        return 0;
    }

    default long visit(T param, ConstructorElementHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, DataDeclarationHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, DataHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ElementOf node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ExactMethodElementHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, FunctionDeclarationHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, FunctionElementHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, FunctionHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, GlobalVariable node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, InstanceFieldOf node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, InterfaceMethodElementHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, VirtualMethodElementHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, LocalVariable node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, MemberOf node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, StaticField node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, StaticMethodElementHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, UnsafeHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, PointerHandle node) {
        return visitUnknown(param, node);
    }

    default long visit(T param, ReferenceHandle node) {
        return visitUnknown(param, node);
    }

    interface Delegating<T> extends ValueHandleVisitorLong<T> {
        ValueHandleVisitorLong<T> getDelegateValueHandleVisitor();

        @Override
        default long visitUnknown(T param, ValueHandle node) {
            return node.accept(getDelegateValueHandleVisitor(), param);
        }

        @Override
        default long visit(T param, ConstructorElementHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, DataDeclarationHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, DataHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, ElementOf node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, ExactMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, FunctionDeclarationHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, FunctionElementHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, FunctionHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, GlobalVariable node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, InstanceFieldOf node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, InterfaceMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, VirtualMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, LocalVariable node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, MemberOf node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, StaticField node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, StaticMethodElementHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, UnsafeHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, PointerHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default long visit(T param, ReferenceHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }
    }
}
