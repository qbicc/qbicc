package cc.quarkus.qcc.graph;

/**
 *
 */
public interface ValueHandleVisitor<T, R> {
    default R visitUnknown(T param, ValueHandle node) {
        return null;
    }

    default R visit(T param, ElementOf node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, GlobalVariable node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, InstanceFieldOf node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, LocalVariable node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, MemberOf node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, StaticField node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, PointerHandle node) {
        return visitUnknown(param, node);
    }

    default R visit(T param, ReferenceHandle node) {
        return visitUnknown(param, node);
    }

    interface Delegating<T, R> extends ValueHandleVisitor<T, R> {
        ValueHandleVisitor<T, R> getDelegateValueHandleVisitor();

        @Override
        default R visitUnknown(T param, ValueHandle node) {
            return node.accept(getDelegateValueHandleVisitor(), param);
        }

        @Override
        default R visit(T param, ElementOf node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default R visit(T param, GlobalVariable node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default R visit(T param, InstanceFieldOf node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default R visit(T param, LocalVariable node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default R visit(T param, MemberOf node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default R visit(T param, StaticField node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default R visit(T param, PointerHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }

        @Override
        default R visit(T param, ReferenceHandle node) {
            return getDelegateValueHandleVisitor().visit(param, node);
        }
    }
}
