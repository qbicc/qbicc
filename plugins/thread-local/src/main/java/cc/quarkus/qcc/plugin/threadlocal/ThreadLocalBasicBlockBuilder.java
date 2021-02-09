package cc.quarkus.qcc.plugin.threadlocal;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
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
    public Node writeStaticField(FieldElement fieldElement, Value value, JavaAccessMode mode) {
        boolean isTL = fieldElement.hasAllModifiersOf(ClassFile.I_ACC_THREAD_LOCAL);
        if (getCurrentElement() instanceof InitializerElement) {
            if (isTL) {
                ctxt.warning(fieldElement, "Initialization of thread locals is not yet supported");
                return nop();
            }
        }
        if (isTL) {
            ThreadLocals threadLocals = ThreadLocals.get(ctxt);
            FieldElement threadLocalField = threadLocals.getThreadLocalField(fieldElement);
            if (threadLocalField == null) {
                ctxt.error(fieldElement, "Internal: Thread local field was not registered");
                return super.writeStaticField(fieldElement, value, mode);
            }
            BasicBlockBuilder b = getFirstBuilder();
            // thread local values are never visible outside of the current thread
            return b.writeInstanceField(currentThread(), threadLocalField, value, JavaAccessMode.PLAIN);
        } else {
            return super.writeStaticField(fieldElement, value, mode);
        }
    }

    @Override
    public Value readStaticField(FieldElement fieldElement, JavaAccessMode mode) {
        boolean isTL = fieldElement.hasAllModifiersOf(ClassFile.I_ACC_THREAD_LOCAL);
        if (getCurrentElement() instanceof InitializerElement) {
            if (isTL) {
                ctxt.warning(fieldElement, "Initialization of thread locals is not yet supported");
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(fieldElement.getType(List.of()));
            }
        }
        if (isTL) {
            ThreadLocals threadLocals = ThreadLocals.get(ctxt);
            FieldElement threadLocalField = threadLocals.getThreadLocalField(fieldElement);
            if (threadLocalField == null) {
                ctxt.error(fieldElement, "Internal: Thread local field was not registered");
                return super.readStaticField(fieldElement, mode);
            }
            BasicBlockBuilder b = getFirstBuilder();
            // thread local values are never visible outside of the current thread
            return b.readInstanceField(currentThread(), threadLocalField, JavaAccessMode.PLAIN);
        }
        return super.readStaticField(fieldElement, mode);
    }
}
