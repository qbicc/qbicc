package org.qbicc.plugin.native_;

import org.qbicc.context.ClassContext;
import org.qbicc.runtime.ExtModifier;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.NativeMethodConfigurator;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * A configurator which allows native method bodies to be defined in a stub class.
 */
public final class NativeBindingMethodConfigurator implements NativeMethodConfigurator {
    private final NativeMethodConfigurator delegate;

    public NativeBindingMethodConfigurator(NativeMethodConfigurator delegate) {
        this.delegate = delegate;
    }

    @Override
    public void configureNativeMethod(MethodElement.Builder builder, DefinedTypeDefinition enclosing, String name, MethodDescriptor methodDescriptor) {
        String internalName = enclosing.getInternalName();
        boolean isNativeStub = internalName.endsWith("$_native");
        if (! isNativeStub) {
            // check to see there are native bindings for it
            ClassContext classContext = enclosing.getContext();
            DefinedTypeDefinition nativeType = classContext.findDefinedType(internalName + "$_native");
            if (nativeType != null) {
                // found it; see if it has a matching method
                LoadedTypeDefinition loaded = nativeType.load();
                int methodIndex = loaded.findMethodIndex(name, methodDescriptor, true);
                if (methodIndex != -1) {
                    // match!
                    MethodElement methodElement = loaded.getMethod(methodIndex);
                    // make sure the stack trace makes sense
                    builder.setSourceFileName(methodElement.getSourceFileName());
                    builder.setSafePointBehavior(methodElement.safePointBehavior());
                    builder.setSafePointSetBits(methodElement.safePointSetBits());
                    builder.setSafePointClearBits(methodElement.safePointClearBits());
                    builder.setMinimumLineNumber(methodElement.getMinimumLineNumber());
                    builder.setMaximumLineNumber(methodElement.getMaximumLineNumber());
                    builder.dropModifiers(ExtModifier.ACC_NATIVE);
                    builder.setMethodBodyFactory(methodElement.getMethodBodyFactory(), methodElement.getMethodBodyFactoryIndex());
                    return;
                }
            }
        }

        // no luck
        delegate.configureNativeMethod(builder, enclosing, name, methodDescriptor);
    }
}
