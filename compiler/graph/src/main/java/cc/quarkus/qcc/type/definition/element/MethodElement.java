package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.descriptor.MethodTypeDescriptor;

/**
 *
 */
public interface MethodElement extends ExactExecutableElement, VirtualExecutableElement, ParameterizedExecutableElement,
                                       AnnotatedElement, NamedElement {
    MethodElement[] NO_METHODS = new MethodElement[0];

    default boolean isAbstract() {
        return hasAllModifiersOf(ClassFile.ACC_ABSTRACT);
    }

    Type getReturnType();

    default MethodTypeDescriptor getMethodTypeDescriptor() {
        int cnt = getParameterCount();
        Type[] args = new Type[cnt];
        for (int i = 0; i < cnt; i ++) {
            args[i] = getParameter(i).getType();
        }
        return MethodTypeDescriptor.of(getReturnType(), args);
    }

    default MethodIdentifier getMethodIdentifier() {
        return MethodIdentifier.of(getName(), getMethodTypeDescriptor());
    }

    interface TypeResolver {
        Type resolveMethodReturnType(long argument) throws ResolutionFailedException;

        // todo: generic/annotated type
    }

    static Builder builder() {
        return new MethodElementImpl.Builder();
    }

    interface Builder extends ExactExecutableElement.Builder, VirtualExecutableElement.Builder, ParameterizedExecutableElement.Builder,
                              AnnotatedElement.Builder, NamedElement.Builder {
        void setReturnTypeResolver(TypeResolver resolver, long argument);

        MethodElement build();
    }
}
