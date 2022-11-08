package org.qbicc.plugin.trycatch;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.plugin.patcher.Patcher;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.generic.TypeSignature;

/**
 * Implementation of the exception-object-on-thread propagation strategy.
 */
public final class ExceptionOnThreadStrategy {
    private ExceptionOnThreadStrategy() {}

    static final String THREAD_INT_NAME = "java/lang/Thread";
    private static final String THROWABLE_INT_NAME = "java/lang/Throwable";

    public static void initialize(CompilationContext ctxt) {
        ClassContext classContext = ctxt.getBootstrapClassContext();
        Patcher patcher = Patcher.get(ctxt);

        // inject the thrown exception field
        ClassTypeDescriptor throwableDesc = ClassTypeDescriptor.synthesize(classContext, THROWABLE_INT_NAME);
        patcher.addField(classContext, THREAD_INT_NAME, "thrown", throwableDesc, new FieldResolver() {
            @Override
            public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
                builder.setEnclosingType(enclosing);
                builder.setSignature(TypeSignature.synthesize(classContext, throwableDesc));
                return builder.build();
            }
        }, 0, 0);
    }

    public static BasicBlockBuilder loweringBuilder(BasicBlockBuilder.FactoryContext ctxt, BasicBlockBuilder delegate) {
        return new ExceptionOnThreadBasicBlockBuilder(delegate);
    }
}
