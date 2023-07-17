package org.qbicc.plugin.threadlocal;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.StaticFieldElement;

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
    public Value load(Value handle, ReadAccessMode accessMode) {
        return super.load(transform(handle), accessMode);
    }

    @Override
    public Node store(Value handle, Value value, WriteAccessMode accessMode) {
        return super.store(transform(handle), value, accessMode);
    }

    @Override
    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return super.readModifyWrite(transform(pointer), op, update, readMode, writeMode);
    }

    @Override
    public Value cmpAndSwap(Value handle, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        return super.cmpAndSwap(transform(handle), expect, update, readMode, writeMode, strength);
    }

    private Value transform(Value pointer) {
        if (pointer instanceof StaticFieldLiteral sfl) {
            StaticFieldElement fieldElement = sfl.getVariableElement();
            boolean isTL = fieldElement.hasAllModifiersOf(ClassFile.I_ACC_THREAD_LOCAL);
            if (element() instanceof InitializerElement) {
                if (isTL) {
                    ctxt.warning(fieldElement, "Initialization of thread locals is not yet supported");
                    return getLiteralFactory().literalOf(fieldElement);
                }
            }
            if (isTL) {
                ThreadLocals threadLocals = ThreadLocals.get(ctxt);
                InstanceFieldElement threadLocalField = threadLocals.getThreadLocalField(fieldElement);
                if (threadLocalField == null) {
                    ctxt.error(fieldElement, "Internal: Thread local field was not registered");
                    return getLiteralFactory().literalOf(fieldElement);
                }
                // thread local values are never visible outside of the current thread
                return instanceFieldOf(load(currentThread(), SingleUnshared), threadLocalField);
            } else {
                return pointer;
            }
        } else {
            return pointer;
        }
    }
}
