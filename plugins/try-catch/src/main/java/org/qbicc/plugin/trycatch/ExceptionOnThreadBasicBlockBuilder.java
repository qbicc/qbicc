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
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.definition.element.InstanceFieldElement;

/**
 * A basic block builder which implements the exception handling strategy of storing the exception on the thread.
 */
final class ExceptionOnThreadBasicBlockBuilder extends DelegatingBasicBlockBuilder {

    private final InstanceFieldElement exceptionField;
    private final Map<BlockLabel, BlockLabel> landingPads = new HashMap<>();

    ExceptionOnThreadBasicBlockBuilder(BasicBlockBuilder delegate) {
        super(delegate);
        ClassContext bcc = delegate.getContext().getBootstrapClassContext();
        exceptionField = bcc.findDefinedType(ExceptionOnThreadStrategy.THREAD_INT_NAME).load().findInstanceField("thrown", true);
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(targetPtr, receiver, arguments, getLandingPad(catchLabel), targetArguments);
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(targetPtr, receiver, arguments, getLandingPad(catchLabel), resumeLabel, targetArguments);
    }

    private BlockLabel getLandingPad(final BlockLabel catchLabel) {
        return landingPads.computeIfAbsent(catchLabel, ExceptionOnThreadBasicBlockBuilder::newLabel);
    }

    private static BlockLabel newLabel(final Object ignored) {
        return new BlockLabel();
    }

    @Override
    public BasicBlock throw_(Value value) {
        store(instanceFieldOf(decodeReference(load(currentThread(), SingleUnshared)), exceptionField), value, SingleUnshared);
        return super.throw_(value);
    }

    @Override
    public void finish() {
        // generate landing pads
        LiteralFactory lf = getLiteralFactory();
        for (Map.Entry<BlockLabel, BlockLabel> entry : landingPads.entrySet()) {
            BlockLabel delegateCatch = entry.getKey();
            BlockLabel landingPad = entry.getValue();
            begin(landingPad, sb -> {
                Value ex = sb.readModifyWrite(instanceFieldOf(decodeReference(load(currentThread(), SingleUnshared)), exceptionField), ReadModifyWrite.Op.SET, lf.zeroInitializerLiteralOfType(exceptionField.getType()), SingleUnshared, SingleUnshared);
                sb.goto_(delegateCatch, Slot.thrown(), ex);
            });
        }
        super.finish();
    }
}
