package org.qbicc.plugin.patcher;

import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.InitializerElement;

public final class OnceRunTimeInitializerResolver implements InitializerResolver {
    private final InitializerResolver delegate;
    private volatile InitializerElement element;

    public OnceRunTimeInitializerResolver(InitializerResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public InitializerElement resolveInitializer(int index, DefinedTypeDefinition enclosing, InitializerElement.Builder builder) {
        InitializerElement element = this.element;
        if (element == null) {
            synchronized (this) {
                element = this.element;
                if (element == null) {
                    element = this.element = delegate.resolveInitializer(index, enclosing, new InitializerElement.Builder.Delegating() {
                        @Override
                        public InitializerElement.Builder getDelegate() {
                            return builder;
                        }

                        @Override
                        public void setModifiers(int modifiers) {
                            getDelegate().setModifiers(modifiers | ClassFile.I_ACC_RUN_TIME);
                        }
                    });
                }
            }
        }
        return element;
    }
}
