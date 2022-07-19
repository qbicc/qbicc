package org.qbicc.plugin.threadlocal;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.PointerValue;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

/**
 *
 */
public class ThreadLocalBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThreadLocalBasicBlockBuilder(FactoryContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public PointerValue staticField(FieldElement fieldElement) {
        boolean isTL = fieldElement.hasAllModifiersOf(ClassFile.I_ACC_THREAD_LOCAL);
        if (getCurrentElement() instanceof InitializerElement) {
            if (isTL) {
                ctxt.warning(fieldElement, "Initialization of thread locals is not yet supported");
                return super.staticField(fieldElement);
            }
        }
        if (isTL) {
            ThreadLocals threadLocals = ThreadLocals.get(ctxt);
            FieldElement threadLocalField = threadLocals.getThreadLocalField(fieldElement);
            if (threadLocalField == null) {
                ctxt.error(fieldElement, "Internal: Thread local field was not registered");
                return super.staticField(fieldElement);
            }
            BasicBlockBuilder b = getFirstBuilder();
            // thread local values are never visible outside of the current thread
            return instanceFieldOf(decodeReference(load(currentThread(), SingleUnshared)), threadLocalField);
        } else {
            return super.staticField(fieldElement);
        }
    }
}
