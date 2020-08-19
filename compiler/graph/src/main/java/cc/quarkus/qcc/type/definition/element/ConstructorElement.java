package cc.quarkus.qcc.type.definition.element;

/**
 *
 */
public interface ConstructorElement extends ParameterizedExecutableElement, AnnotatedElement {
    ConstructorElement[] NO_CONSTRUCTORS = new ConstructorElement[0];

    static Builder builder() {
        return new ConstructorElementImpl.Builder();
    }

    interface Builder extends ParameterizedExecutableElement.Builder, AnnotatedElement.Builder {
        ConstructorElement build();
    }
}
