package org.qbicc.plugin.gc.common.safepoint;

import java.util.List;
import java.util.Map;

import org.eclipse.collections.api.factory.Maps;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.VoidType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Block builder which places safepoint polls.
 */
public final class SafePointPlacementBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final SafePointBehavior ourBehavior;

    private SafePointPlacementBasicBlockBuilder(BasicBlockBuilder delegate, SafePointBehavior behavior) {
        super(delegate);
        ourBehavior = behavior;
    }

    public static BasicBlockBuilder createIfNeeded(FactoryContext ctxt, BasicBlockBuilder delegate) {
        final ExecutableElement currentElement = delegate.element();
        SafePointBehavior behavior = currentElement.safePointBehavior();
        return new SafePointPlacementBasicBlockBuilder(delegate, behavior);
    }

    @Override
    public BasicBlock return_(Value value) {
        if (ourBehavior == SafePointBehavior.POLLING) {
            // todo: make sure last instruction wasn't a call or safepoint poll, enter, or exit
            pollSafePoint();
        }
        return super.return_(value);
    }

    @Override
    public BasicBlock throw_(Value value) {
        if (ourBehavior == SafePointBehavior.POLLING) {
            // todo: make sure last instruction wasn't a call or safepoint poll, enter, or exit
            pollSafePoint();
        } else if (ourBehavior.isInSafePoint()) {
            // cannot throw from here, so trap instead
            return unreachable();
            // just fall through, it doesn't matter
        }
        return super.throw_(value);
    }

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        SafePointBehavior calleeBehavior = targetPtr.safePointBehavior();
        if (! ourBehavior.mayCall(calleeBehavior)) {
            // ERROR
            throw mayNotCall(targetPtr);
        } else if (ourBehavior.isNotInSafePoint() && calleeBehavior == SafePointBehavior.ENTER) {
            // enter safepoint for duration of method
            LiteralFactory lf = getLiteralFactory();
            enterSafePoint(lf.literalOf(targetPtr.safePointSetBits()), lf.literalOf(targetPtr.safePointClearBits()));
            Value result = super.call(targetPtr, receiver, arguments);
            exitSafePoint(lf.literalOf(targetPtr.safePointClearBits()), lf.literalOf(targetPtr.safePointSetBits()));
            return result;
        } else if (ourBehavior.isInSafePoint() && calleeBehavior == SafePointBehavior.EXIT) {
            // exit safepoint for duration of method
            LiteralFactory lf = getLiteralFactory();
            exitSafePoint(lf.literalOf(targetPtr.safePointSetBits()), lf.literalOf(targetPtr.safePointClearBits()));
            Value result = super.call(targetPtr, receiver, arguments);
            enterSafePoint(lf.literalOf(targetPtr.safePointClearBits()), lf.literalOf(targetPtr.safePointSetBits()));
            return result;
        } else {
            return super.call(targetPtr, receiver, arguments);
        }
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        SafePointBehavior calleeBehavior = targetPtr.safePointBehavior();
        if (! ourBehavior.mayCall(calleeBehavior)) {
            // ERROR
            throw mayNotCall(targetPtr);
        } else if (ourBehavior.isNotInSafePoint() && calleeBehavior == SafePointBehavior.ENTER ||
            ourBehavior.isInSafePoint() && calleeBehavior == SafePointBehavior.EXIT) {
            getContext().error(getLocation(), "Can not enter or exit safepoint around no-side-effects call");
        }
        return super.callNoSideEffects(targetPtr, receiver, arguments);
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        SafePointBehavior calleeBehavior = targetPtr.safePointBehavior();
        if (! ourBehavior.mayCall(calleeBehavior)) {
            // ERROR
            return mayNotCall(targetPtr).getTerminatedBlock();
        } else if (ourBehavior.isNotInSafePoint() && calleeBehavior == SafePointBehavior.ENTER) {
            // enter safepoint for duration of method
            LiteralFactory lf = getLiteralFactory();
            enterSafePoint(lf.literalOf(targetPtr.safePointSetBits()), lf.literalOf(targetPtr.safePointClearBits()));
            return super.callNoReturn(targetPtr, receiver, arguments);
        } else if (ourBehavior.isInSafePoint() && calleeBehavior == SafePointBehavior.EXIT) {
            // exit safepoint for duration of method
            LiteralFactory lf = getLiteralFactory();
            exitSafePoint(lf.literalOf(targetPtr.safePointSetBits()), lf.literalOf(targetPtr.safePointClearBits()));
            return super.callNoReturn(targetPtr, receiver, arguments);
        } else {
            return super.callNoReturn(targetPtr, receiver, arguments);
        }
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        SafePointBehavior calleeBehavior = targetPtr.safePointBehavior();
        if (! ourBehavior.mayCall(calleeBehavior)) {
            // ERROR
            return mayNotCall(targetPtr).getTerminatedBlock();
        } else if (calleeBehavior == SafePointBehavior.ENTER && ourBehavior.isNotInSafePoint()
            || calleeBehavior == SafePointBehavior.EXIT && ourBehavior.isInSafePoint()) {
            // undo the tail call
            return return_(call(targetPtr, receiver, arguments));
        }
        return super.tailCall(targetPtr, receiver, arguments);
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        if (! ourBehavior.mayCall(targetPtr.safePointBehavior())) {
            // ERROR
            throw mayNotCall(targetPtr);
        } else if (targetPtr.safePointBehavior() == SafePointBehavior.ENTER || ourBehavior.isInSafePoint()) {
            // cannot throw under these circumstances
            Value result = call(targetPtr, receiver, arguments);
            if (result.getType() instanceof VoidType) {
                goto_(resumeLabel, targetArguments);
                return emptyVoid();
            } else {
                targetArguments = Maps.immutable.ofMap(targetArguments).newWithKeyValue(Slot.result(), result).castToMap();
                goto_(resumeLabel, targetArguments);
                return addParam(resumeLabel, Slot.result(), result.getType());
            }
        }
        return super.invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
    }

    private BlockEarlyTermination mayNotCall(final Value targetPtr) {
        getContext().error(getLocation(), "Method with safepoint behavior %s may not call a method (%s) with behavior %s", ourBehavior, targetPtr, targetPtr.safePointBehavior());
        return new BlockEarlyTermination(unreachable());
    }
}
