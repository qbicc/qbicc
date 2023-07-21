package org.qbicc.plugin.trycatch;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.VoidType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A basic block builder which adds a monitor acquire to the start of the subprogram and adds a monitor release
 * to all possible exit paths.
 */
public class SynchronizedMethodBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private static final Slot THROWN = Slot.thrown();
    private Value monitor;
    private final ReferenceType throwable;
    private boolean started;

    private SynchronizedMethodBasicBlockBuilder(final BasicBlockBuilder delegate) {
        super(delegate);
        throwable = getContext().getBootstrapClassContext().findDefinedType("java/lang/Throwable").load().getClassType().getReference();
    }

    @Override
    public Node begin(BlockLabel blockLabel) {
        Node node = super.begin(blockLabel);
        if (! started) {
            started = true;
            ExecutableElement element = element();
            DefinedTypeDefinition enclosing = element.getEnclosingType();
            if (element.isStatic()) {
                monitor = classOf(enclosing.load().getObjectType());
            } else {
                monitor = addParam(blockLabel, Slot.this_(), enclosing.load().getObjectType().getReference(), false);
            }
            monitorEnter(monitor);
        }
        return node;
    }

    @Override
    public <T> BasicBlock begin(BlockLabel blockLabel, T arg, BiConsumer<T, BasicBlockBuilder> maker) {
        if (! started) {
            ExecutableElement element = element();
            DefinedTypeDefinition enclosing = element.getEnclosingType();
            if (element.isStatic()) {
                monitor = classOf(enclosing.load().getObjectType());
            } else {
                monitor = addParam(blockLabel, Slot.this_(), enclosing.load().getObjectType().getReference(), false);
            }
            // method start
            return super.begin(blockLabel, bbb -> {
                started = true;
                monitorEnter(monitor);
                maker.accept(arg, bbb);
            });
        } else {
            return super.begin(blockLabel, arg, maker);
        }
    }

    public BasicBlock return_(final Value value) {
        monitorExit(monitor);
        return super.return_(value);
    }

    public BasicBlock throw_(final Value value) {
        monitorExit(monitor);
        return super.throw_(value);
    }

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        if (targetPtr.getPointeeType() instanceof FunctionType) {
            // invoke is forbidden
            return super.call(targetPtr, receiver, arguments);
        }
        BlockLabel resumeLabel = new BlockLabel();
        BlockLabel handlerLabel = new BlockLabel();
        Value rv = invoke(targetPtr, receiver, arguments, BlockLabel.of(begin(handlerLabel, ignored -> throw_(addParam(handlerLabel, THROWN, throwable, false)))), resumeLabel, Map.of());
        begin(resumeLabel);
        return rv.getType() instanceof VoidType ? emptyVoid() : addParam(resumeLabel, Slot.result(), rv.getType());
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        if (targetPtr.getPointeeType() instanceof FunctionType) {
            // invoke is forbidden
            return super.callNoReturn(targetPtr, receiver, arguments);
        }
        BlockLabel handlerLabel = new BlockLabel();
        return invokeNoReturn(targetPtr, receiver, arguments, BlockLabel.of(begin(handlerLabel, ignored -> throw_(addParam(handlerLabel, THROWN, throwable, false)))), Map.of());
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        // tail calls don't work with synchronized
        BasicBlockBuilder fb = getFirstBuilder();
        return fb.return_(fb.call(targetPtr, receiver, arguments));
    }

    public static BasicBlockBuilder createIfNeeded(FactoryContext fc, BasicBlockBuilder delegate) {
        if (delegate.element().hasAllModifiersOf(ClassFile.ACC_SYNCHRONIZED)) {
            return new SynchronizedMethodBasicBlockBuilder(delegate);
        } else {
            return delegate;
        }
    }
}
