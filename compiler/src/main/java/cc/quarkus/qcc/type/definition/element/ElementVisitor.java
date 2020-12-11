package cc.quarkus.qcc.type.definition.element;

/**
 *
 */
public interface ElementVisitor<T, R> {
    default R visitUnknown(T param, BasicElement element) {
        return null;
    }

    default R visit(T param, ConstructorElement element) {
        return visitUnknown(param, element);
    }

    default R visit(T param, FieldElement element) {
        return visitUnknown(param, element);
    }

    default R visit(T param, InitializerElement element) {
        return visitUnknown(param, element);
    }

    default R visit(T param, LocalVariableElement element) {
        return visitUnknown(param, element);
    }

    default R visit(T param, MethodElement element) {
        return visitUnknown(param, element);
    }

    default R visit(T param, ParameterElement element) {
        return visitUnknown(param, element);
    }

    interface Delegating<T, R> extends ElementVisitor<T, R> {

        ElementVisitor<? super T, ? extends R> getDelegateElementVisitor();

        default R visitUnknown(final T param, final BasicElement element) {
            return element.accept(getDelegateElementVisitor(), param);
        }

        default R visit(final T param, final ConstructorElement element) {
            return getDelegateElementVisitor().visit(param, element);
        }

        default R visit(final T param, final LocalVariableElement element) {
            return getDelegateElementVisitor().visit(param, element);
        }

        default R visit(final T param, final MethodElement element) {
            return getDelegateElementVisitor().visit(param, element);
        }

        default R visit(final T param, final InitializerElement element) {
            return getDelegateElementVisitor().visit(param, element);
        }

        default R visit(final T param, final FieldElement element) {
            return getDelegateElementVisitor().visit(param, element);
        }

        default R visit(final T param, final ParameterElement element) {
            return getDelegateElementVisitor().visit(param, element);
        }
    }
}
