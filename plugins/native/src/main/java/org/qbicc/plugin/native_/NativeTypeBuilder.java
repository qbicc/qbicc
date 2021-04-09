package org.qbicc.plugin.native_;

import java.util.concurrent.atomic.AtomicReference;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.definition.ConstructorResolver;
import org.qbicc.type.definition.DefinedTypeDefinition;

/**
 *
 */
public class NativeTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition.Builder delegate;
    private boolean isNative;

    public NativeTypeBuilder(final ClassContext classCtxt, final DefinedTypeDefinition.Builder delegate) {
        this.classCtxt = classCtxt;
        this.ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    public void setSuperClassName(final String superClassInternalName) {
        if (superClassInternalName != null) {
            if (superClassInternalName.equals(Native.OBJECT_INT_NAME) || superClassInternalName.equals(Native.WORD_INT_NAME) || superClassInternalName.equals(Native.PTR_INT_NAME)) {
                // probe native object type
                isNative = true;
            }
        }
        getDelegate().setSuperClassName(superClassInternalName);
    }

    public void addConstructor(final ConstructorResolver resolver, final int index) {
        // native types cannot be constructed the normal way
        if (! isNative) {
            delegate.addConstructor(resolver, index);
        }
    }

    public DefinedTypeDefinition build() {
        // wrap up
        DefinedTypeDefinition builtType = getDelegate().build();
        if (isNative && ! builtType.isAbstract()) {
            NativeInfo.get(ctxt).nativeTypes.put(builtType, new AtomicReference<>());
        }
        return builtType;
    }
}
