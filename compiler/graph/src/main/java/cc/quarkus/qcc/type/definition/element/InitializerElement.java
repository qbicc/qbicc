package cc.quarkus.qcc.type.definition.element;

/**
 *
 */
public interface InitializerElement extends ExecutableElement {

    static Builder builder() {
        return new InitializerElementImpl.Builder();
    }

    interface Builder extends ExecutableElement.Builder {
        InitializerElement build();
    }
}
