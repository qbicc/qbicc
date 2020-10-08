package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import cc.quarkus.qcc.type.descriptor.ConstructorDescriptor;

/**
 *
 */
public interface ConstructorElement extends ParameterizedExecutableElement, AnnotatedElement {
    ConstructorElement[] NO_CONSTRUCTORS = new ConstructorElement[0];

    ConstructorDescriptor getDescriptor();

    static Builder builder() {
        return new ConstructorElementImpl.Builder();
    }

    interface TypeResolver {
        ConstructorDescriptor resolveConstructorDescriptor(int argument) throws ResolutionFailedException;

        // todo: generic/annotated type
    }

    interface Builder extends ParameterizedExecutableElement.Builder, AnnotatedElement.Builder {
        void setConstructorTypeResolver(TypeResolver resolver, int argument);

        ConstructorElement build();
    }
}
