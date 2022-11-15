package org.qbicc.plugin.threadlocal;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.StaticFieldElement;

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
    public Value load(ValueHandle handle, ReadAccessMode accessMode) {
        return super.load(transform(handle), accessMode);
    }

    @Override
    public Node store(ValueHandle handle, Value value, WriteAccessMode accessMode) {
        return super.store(transform(handle), value, accessMode);
    }

    @Override
    public Value readModifyWrite(ValueHandle handle, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return super.readModifyWrite(transform(handle), op, update, readMode, writeMode);
    }

    @Override
    public Value cmpAndSwap(ValueHandle handle, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        return super.cmpAndSwap(transform(handle), expect, update, readMode, writeMode, strength);
    }

    private ValueHandle transform(ValueHandle handle) {
        if (handle instanceof PointerHandle ph && ph.getPointerValue() instanceof StaticFieldLiteral sfl) {
            StaticFieldElement fieldElement = sfl.getVariableElement();
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
                // thread local values are never visible outside of the current thread
                return instanceFieldOf(referenceHandle(load(currentThread(), SingleUnshared)), threadLocalField);
            } else {
                return handle;
            }
        } else {
            return handle;
        }
    }
}
