package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;

/**
 *
 */
public interface ParameterElement extends NamedElement, AnnotatedElement {
    ParameterElement[] NO_PARAMETERS = new ParameterElement[0];

    Type getType();

    interface TypeResolver {
        Type resolveParameterType(long argument) throws ResolutionFailedException;

        // todo: generic/annotated type
    }

    static Builder builder() {
        return new ParameterElementImpl.Builder();
    }

    interface Builder extends NamedElement.Builder, AnnotatedElement.Builder {
        void setResolver(TypeResolver resolver, long argument);

        ParameterElement build();
    }
}
