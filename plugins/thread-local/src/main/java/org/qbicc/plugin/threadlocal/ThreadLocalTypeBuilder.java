package org.qbicc.plugin.threadlocal;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;

/**
 *
 */
public class ThreadLocalTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DefinedTypeDefinition.Builder delegate;

    public ThreadLocalTypeBuilder(final ClassContext classCtxt, final DefinedTypeDefinition.Builder delegate) {
        this.classCtxt = classCtxt;
        this.ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    @Override
    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    @Override
    public void addField(FieldResolver resolver, int index) {
        FieldResolver ourResolver = new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing) {
                FieldElement resolved = resolver.resolveField(index, enclosing);
                List<Annotation> annotations = resolved.getInvisibleAnnotations();
                for (Annotation annotation : annotations) {
                    ClassTypeDescriptor desc = annotation.getDescriptor();
                    if (desc.getPackageName().equals("org/qbicc/runtime") && desc.getClassName().equals("ThreadScoped")) {
                        if (! resolved.isStatic()) {
                            ctxt.error(resolved, "Thread-local fields must be `static`");
                        } else if (resolved.isVolatile()) {
                            ctxt.error(resolved, "Thread-local fields must not be `volatile`");
                        } else {
                            if (resolved.isFinal()) {
                                ctxt.warning(resolved, "Initialization of thread locals is not yet supported");
                            }
                            resolved.setModifierFlags(ClassFile.I_ACC_THREAD_LOCAL);
                            DefinedTypeDefinition jlt = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Thread");
                            FieldElement.Builder injectedFieldBuilder = FieldElement.builder();
                            int modifiers = resolved.getModifiers();
                            // remove unwanted modifiers
                            modifiers &= ~(ClassFile.I_ACC_THREAD_LOCAL | ClassFile.ACC_STATIC);
                            // add these modifiers
                            modifiers |= ClassFile.I_ACC_NO_RESOLVE | ClassFile.I_ACC_NO_REFLECT;
                            injectedFieldBuilder.setModifiers(modifiers);
                            injectedFieldBuilder.setName(resolved.getName());
                            injectedFieldBuilder.setSourceFileName(resolved.getSourceFileName());
                            injectedFieldBuilder.setDescriptor(resolved.getTypeDescriptor());
                            injectedFieldBuilder.setSignature(resolved.getTypeSignature());
                            injectedFieldBuilder.setEnclosingType(jlt);
                            FieldElement injectedField = injectedFieldBuilder.build();
                            jlt.load().injectField(injectedField);
                            ThreadLocals.get(ctxt).registerThreadLocalField(resolved, injectedField);
                        }
                    }
                }
                return resolved;
            }
        };
        delegate.addField(ourResolver, index);
    }
}
