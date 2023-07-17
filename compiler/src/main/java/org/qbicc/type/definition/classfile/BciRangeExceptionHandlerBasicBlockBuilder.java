package org.qbicc.type.definition.classfile;

import java.util.List;
import java.util.Map;

import org.eclipse.collections.api.factory.Maps;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.generic.TypeParameterContext;

/**
 * A basic block builder that catches a single exception by type and bytecode index range.
 */
public final class BciRangeExceptionHandlerBasicBlockBuilder extends DelegatingBasicBlockBuilder  {
    private static final Slot THROWN = Slot.thrown();
    private static final Slot STACK0 = Slot.stack(0);

    private final int startBci, endBci;

    private final int handlerBci;
    private final ClassObjectType exType;
    private final ClassObjectType throwableType;
    private final MethodParser mp;

    /**
     * Construct a new instance.
     *
     * @param delegate the next basic block builder (must not be {@code null})
     * @param startBci the start of the bytecode range for which this exception handler is valid (inclusive)
     * @param endBci the end of the bytecode range for which this exception handler is valid (exclusive)
     * @param handlerBci the catch handler bytecode index for the exception type and range
     * @param exType the exception type (must not be {@code null})
     * @param mp the method parser (must not be {@code null})
     */
    private BciRangeExceptionHandlerBasicBlockBuilder(BasicBlockBuilder delegate, int startBci, int endBci, int handlerBci, ClassObjectType exType, MethodParser mp) {
        super(delegate);
        this.startBci = startBci;
        this.endBci = endBci;
        this.handlerBci = handlerBci;
        this.exType = exType;
        throwableType = getContext().getBootstrapClassContext().findDefinedType("java/lang/Throwable").load().getClassType();
        this.mp = mp;
    }

    private boolean inRange() {
        int bci = bytecodeIndex();
        return startBci <= bci && bci < endBci;
    }

    @Override
    public BasicBlock throw_(Value value) {
        if (inRange()) {
            BlockLabel handler = mp.getOrCreateBlockForIndex(handlerBci);
            return enterHandler(value, handler, mp.captureOutbound());
        }
        return super.throw_(value);
    }

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        if (inRange() && ! targetPtr.isNoThrow() && ! (targetPtr.getPointeeType() instanceof FunctionType)) {
            Map<Slot, Value> capture = mp.captureOutbound();
            BlockLabel resumeLabel = new BlockLabel();
            BlockLabel handlerLabel = new BlockLabel();
            Value rv = invoke(targetPtr, receiver, arguments, BlockLabel.of(begin(handlerLabel, this, (bbb, fb) -> bbb.beginHandler(handlerLabel, capture))), resumeLabel, capture);
            begin(resumeLabel);
            return addParam(resumeLabel, Slot.result(), rv.getType(), rv.isNullable());
        }
        return super.call(targetPtr, receiver, arguments);
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        if (inRange() && ! targetPtr.isNoThrow() && ! (targetPtr.getPointeeType() instanceof FunctionType)) {
            Map<Slot, Value> capture = mp.captureOutbound();
            BlockLabel handlerLabel = new BlockLabel();
            return invokeNoReturn(targetPtr, receiver, arguments, BlockLabel.of(begin(handlerLabel, this, (bbb, fb) -> bbb.beginHandler(handlerLabel, capture))), Map.of());
        }
        return super.callNoReturn(targetPtr, receiver, arguments);
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        if (inRange() && ! targetPtr.isNoThrow() && ! (targetPtr.getPointeeType() instanceof FunctionType)) {
            return_(call(targetPtr, receiver, arguments));
        }
        return super.tailCall(targetPtr, receiver, arguments);
    }

    private void beginHandler(BlockLabel handlerLabel, Map<Slot, Value> capture) {
        enterHandler(addParam(handlerLabel, THROWN, throwableType.getReference(), false), mp.getOrCreateBlockForIndex(handlerBci), capture);
    }

    private BasicBlock enterHandler(final Value ex, final BlockLabel handler, final Map<Slot, Value> args) {
        BasicBlockBuilder fb = getFirstBuilder();
        int oldBci = fb.setBytecodeIndex(handlerBci);
        // todo: temporarily manually narrow the value until we can materialize constraints reliably
        ReferenceType narrowedType = ex.getType(ReferenceType.class).narrow(exType);
        Value narrowed = narrowedType == null ? null : fb.bitCast(ex, narrowedType);
        // user handlers always expect it in stack slot 0
        Map<Slot, Value> adjustedArgs = narrowed == null ? args : Maps.immutable.ofMap(args).newWithKeyValue(STACK0, narrowed).castToMap();
        ReferenceType actualType = ex.getType(ReferenceType.class);
        if (actualType.instanceOf(exType)) {
            // it always matches
            return fb.goto_(handler, adjustedArgs);
        } else if (actualType.getUpperBound().isSupertypeOf(exType)) {
            // it might match if narrowed
            return fb.if_(fb.instanceOf(ex, exType), handler, BlockLabel.of(fb.begin(new BlockLabel(), unused -> {
                fb.setBytecodeIndex(oldBci);
                super.throw_(ex);
            })), adjustedArgs);
        } else {
            assert narrowed == null;
            // it will never match; propagate
            fb.setBytecodeIndex(oldBci);
            return super.throw_(ex);
        }
    }

    /**
     * Wrap the delegate basic block builder with zero or more builders which handle catching the exceptions
     * listed in the element's exception table.
     *
     * @param delegate the delegate basic block builder (must not be {@code null})
     * @return the wrapping basic block builder (not {@code null})
     */
    public static BasicBlockBuilder createIfNeeded(FactoryContext fc, BasicBlockBuilder delegate) {
        if (!fc.has(MethodParser.class)) {
            return delegate;
        }
        MethodParser mp = fc.get(MethodParser.class);
        ClassMethodInfo info = mp.getClassMethodInfo();
        int len = info.getExTableLen();
        for (int i = len - 1; i >= 0; i --) {
            int startPc = info.getExTableEntryStartPc(i);
            int endPc = info.getExTableEntryEndPc(i);
            int handlerPc = info.getExTableEntryHandlerPc(i);
            int typeIdx = info.getExTableEntryTypeIdx(i);
            ClassObjectType exType;
            if (typeIdx == 0) {
                exType = delegate.getCurrentClassContext().findDefinedType("java/lang/Throwable").load().getClassType();
            } else {
                exType = (ClassObjectType) info.getClassFile().getTypeConstant(typeIdx, TypeParameterContext.of(delegate.element()));
            }
            delegate = new BciRangeExceptionHandlerBasicBlockBuilder(delegate, startPc, endPc, handlerPc, exType, mp);
        }
        return delegate;
    }
}
