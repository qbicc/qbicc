package cc.quarkus.qcc.type.definition.element;

/**
 *
 */
public interface InitializerElement extends ExactExecutableElement {

    static Builder builder() {
        return new InitializerElementImpl.Builder();
    }

    interface Builder extends ExactExecutableElement.Builder {
        InitializerElement build();
    }
}
