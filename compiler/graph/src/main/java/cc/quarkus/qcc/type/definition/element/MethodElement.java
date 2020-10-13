package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 *
 */
public interface MethodElement extends ExecutableElement, VirtualExecutableElement, ParameterizedExecutableElement,
                                       AnnotatedElement, NamedElement {
    MethodElement[] NO_METHODS = new MethodElement[0];

    default boolean isAbstract() {
        return hasAllModifiersOf(ClassFile.ACC_ABSTRACT);
    }

    ValueType getReturnType();

    default boolean overrides(MethodElement other) {
        if (other.getReturnType() == getReturnType()) {
            int cnt = getParameterCount();
            if (other.getParameterCount() == cnt) {
                for (int i = 0; i < cnt; i ++) {
                    if (other.getParameter(i).getType() != getParameter(i).getType()) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    MethodDescriptor getDescriptor();

    interface TypeResolver {
        MethodDescriptor resolveMethodDescriptor(int argument) throws ResolutionFailedException;

        // todo: generic/annotated type
    }

    static Builder builder() {
        return new MethodElementImpl.Builder();
    }

    interface Builder extends VirtualExecutableElement.Builder, ParameterizedExecutableElement.Builder,
                              AnnotatedElement.Builder, NamedElement.Builder {
        void setMethodTypeResolver(TypeResolver resolver, int argument);

        MethodElement build();
    }
}
