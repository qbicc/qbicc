package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;

/**
 *
 */
public interface ParameterElement extends NamedElement, AnnotatedElement {
    ParameterElement[] NO_PARAMETERS = new ParameterElement[0];

    int getIndex();

    ValueType getType();

    boolean hasClass2Type();

    interface TypeResolver {
        ValueType resolveParameterType(int methodArg, int paramArg) throws ResolutionFailedException;

        boolean hasClass2ParameterType(int methodArg, int paramArg);

        // todo: generic/annotated type
    }

    static Builder builder() {
        return new ParameterElementImpl.Builder();
    }

    interface Builder extends NamedElement.Builder, AnnotatedElement.Builder {
        void setResolver(TypeResolver resolver, int methodArg, int paramArg);

        void setIndex(int index);

        ParameterElement build();
    }
}
