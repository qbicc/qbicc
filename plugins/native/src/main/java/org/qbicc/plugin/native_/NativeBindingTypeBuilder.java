package org.qbicc.plugin.native_;

import org.jboss.logging.Logger;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public class NativeBindingTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private static final Logger log = Logger.getLogger("org.qbicc.plugin.native_");

    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition.Builder delegate;

    public NativeBindingTypeBuilder(final ClassContext classCtxt, final DefinedTypeDefinition.Builder delegate) {
        this.classCtxt = classCtxt;
        this.ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    @Override
    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    @Override
    public void addMethod(MethodResolver resolver, int index) {
        getDelegate().addMethod(new MethodResolver() {
            @Override
            public MethodElement resolveMethod(int index, DefinedTypeDefinition enclosing) {
                MethodElement origMethod = resolver.resolveMethod(index, enclosing);
                if (origMethod.isNative()) {
                    // check to see there are native bindings for it
                    DefinedTypeDefinition enclosingType = origMethod.getEnclosingType();
                    String internalName = enclosingType.getInternalName();
                    boolean isBinding = internalName.endsWith("$_native");
                    DefinedTypeDefinition nativeType;
                    if (isBinding) {
                        // strip $_native to get the original class
                        nativeType = classCtxt.findDefinedType(internalName.substring(0, internalName.length() - 8));
                    } else {
                        nativeType = classCtxt.findDefinedType(internalName + "$_native");
                    }
                    if (nativeType != null) {
                        // found the native type; check for corresponding method
                        MethodElement nativeMethod = nativeType.load().resolveMethodElementExact(origMethod.getName(), origMethod.getDescriptor());
                        if (nativeMethod != null && nativeMethod.tryCreateMethodBody()) {
                            // there's a match
                            if (isBinding) {
                                // register reverse binding to remap calls
                                NativeInfo.get(ctxt).registerNativeBinding(nativeMethod, origMethod);
                            } else {
                                // plug in replacement method body, let calls flow naturally
                                origMethod.replaceMethodBody(nativeMethod.getMethodBody());
                            }
                        } else {
                            log.debugf("No match found for native method %s in bindings class %s", origMethod, nativeType.getInternalName());
                        }
                    }
                }
                return origMethod;
            }
        }, index);
    }

    @Override
    public void addField(FieldResolver resolver, int index) {
        getDelegate().addField(new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing) {
                FieldElement origField = resolver.resolveField(index, enclosing);
                DefinedTypeDefinition enclosingType = origField.getEnclosingType();
                String internalName = enclosingType.getInternalName();
                boolean isBinding = internalName.endsWith("$_native");
                if (isBinding) {
                    // strip $_native to get the original class
                    DefinedTypeDefinition nativeType = classCtxt.findDefinedType(internalName.substring(0, internalName.length() - 8));
                    if (nativeType != null) {
                        // found it
                        LoadedTypeDefinition loadedNativeType = nativeType.load();
                        FieldElement mappedField = loadedNativeType.resolveField(origField.getTypeDescriptor(), origField.getName());
                        if (mappedField != null) {
                            NativeInfo.get(ctxt).registerNativeBinding(origField, mappedField);
                        }
                    }
                }
                return origField;
            }
        }, index);
    }
}
