package org.qbicc.type.definition;

import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 *
 */
public interface NativeMethodConfigurator {
    /**
     * Configure a native method with a method body factory and possibly other enhancements.
     *
     * @param builder the method element builder (must not be {@code null})
     * @param enclosing the enclosing type of the method (must not be {@code null})
     * @param name the method name (must not be {@code null})
     * @param methodDescriptor the method descriptor (must not be {@code null})
     */
    void configureNativeMethod(MethodElement.Builder builder, DefinedTypeDefinition enclosing, String name, MethodDescriptor methodDescriptor);
}
