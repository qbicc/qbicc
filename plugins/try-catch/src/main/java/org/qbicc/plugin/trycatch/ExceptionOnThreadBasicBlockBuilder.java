package org.qbicc.plugin.trycatch;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qbicc.context.ClassContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.definition.element.FieldElement;

/**
 * A basic block builder which implements the exception handling strategy of storing the exception on the thread.
 */
final class ExceptionOnThreadBasicBlockBuilder extends DelegatingBasicBlockBuilder {

    private final FieldElement exceptionField;
    private final Map<BlockLabel, BlockLabel> landingPads = new HashMap<>();

    ExceptionOnThreadBasicBlockBuilder(BasicBlockBuilder delegate) {
        super(delegate);
        ClassContext bcc = delegate.getContext().getBootstrapClassContext();
        exceptionField = bcc.findDefinedType(ExceptionOnThreadStrategy.THREAD_INT_NAME).load().findField("thrown", true);
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(target, arguments, getLandingPad(catchLabel), targetArguments);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(target, arguments, getLandingPad(catchLabel), resumeLabel, targetArguments);
    }

    private BlockLabel getLandingPad(final BlockLabel catchLabel) {
        return landingPads.computeIfAbsent(catchLabel, ExceptionOnThreadBasicBlockBuilder::newLabel);
    }

    private static BlockLabel newLabel(final Object ignored) {
        return new BlockLabel();
    }

    @Override
    public BasicBlock throw_(Value value) {
        store(instanceFieldOf(referenceHandle(load(pointerHandle(currentThread()), SingleUnshared)), exceptionField), value, SingleUnshared);
        return super.throw_(value);
    }

    @Override
    public void finish() {
        // generate landing pads
        LiteralFactory lf = getLiteralFactory();
        for (Map.Entry<BlockLabel, BlockLabel> entry : landingPads.entrySet()) {
            BlockLabel delegateCatch = entry.getKey();
            BlockLabel landingPad = entry.getValue();
            begin(landingPad);
            Value ex = readModifyWrite(instanceFieldOf(referenceHandle(load(pointerHandle(currentThread()), SingleUnshared)), exceptionField), ReadModifyWrite.Op.SET, lf.zeroInitializerLiteralOfType(exceptionField.getType()), SingleUnshared, SingleUnshared);
            goto_(delegateCatch, Slot.thrown(), ex);
        }
        super.finish();
    }
}
