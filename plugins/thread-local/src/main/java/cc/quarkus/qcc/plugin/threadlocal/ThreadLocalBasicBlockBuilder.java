package cc.quarkus.qcc.plugin.threadlocal;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;

/**
 *
 */
public class ThreadLocalBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ThreadLocalBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public ValueHandle staticField(FieldElement fieldElement) {
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
            return instanceFieldOf(referenceHandle(currentThread()), threadLocalField);
        } else {
            return super.staticField(fieldElement);
        }
    }
}
