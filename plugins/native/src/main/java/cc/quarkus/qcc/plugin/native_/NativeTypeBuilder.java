package cc.quarkus.qcc.plugin.native_;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.AnnotationValue;
import cc.quarkus.qcc.type.annotation.StringAnnotationValue;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodResolver;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public class NativeTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition.Builder delegate;

    public NativeTypeBuilder(final ClassContext classCtxt, final DefinedTypeDefinition.Builder delegate) {
        this.classCtxt = classCtxt;
        this.ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    public void addMethod(final MethodResolver resolver, final int index) {
        delegate.addMethod(new MethodResolver() {
            public MethodElement resolveMethod(final int index, final DefinedTypeDefinition enclosing) {
                NativeInfo nativeInfo = NativeInfo.get(ctxt);
                MethodElement origMethod = resolver.resolveMethod(index, enclosing);
                boolean isNative = origMethod.hasAllModifiersOf(ClassFile.ACC_NATIVE);
                int cnt = origMethod.getVisibleAnnotationCount();
                // look for annotations that indicate that this method requires special handling
                for (int i = 0; i < cnt; i ++) {
                    Annotation annotation = origMethod.getVisibleAnnotation(i);
                    if (annotation.getClassInternalName().equals(Native.ANN_EXTERN)) {
                        AnnotationValue nameVal = annotation.getValue("withName");
                        String name = nameVal == null ? origMethod.getName() : ((StringAnnotationValue) nameVal).getString();
                        // register as a function
                        int pc = origMethod.getParameterCount();
                        ValueType[] types = new ValueType[pc];
                        for (int j = 0; j < pc; j ++) {
                            types[j] = origMethod.getDescriptor().getParameterType(j);
                        }
                        nativeInfo.nativeFunctions.put(origMethod, new NativeFunctionInfo(ctxt.getLiteralFactory().literalOfSymbol(
                            name,
                            ctxt.getTypeSystem().getFunctionType(origMethod.getReturnType(), types)
                        )));
                        // all done
                        break;
                    }
                }
                return origMethod;
            }
        }, index);
    }
}
