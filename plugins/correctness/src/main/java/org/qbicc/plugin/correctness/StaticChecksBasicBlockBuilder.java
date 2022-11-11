package org.qbicc.plugin.correctness;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.FunctionElementHandle;
import org.qbicc.graph.InstanceMethodElementHandle;
import org.qbicc.graph.Slot;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class StaticChecksBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public StaticChecksBasicBlockBuilder(FactoryContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value memberOf(Value structPointer, CompoundType.Member member) {
        if (structPointer.getType(PointerType.class).getPointeeType() instanceof CompoundType) {
            return super.memberOf(structPointer, member);
        }
        ctxt.error(getLocation(), "`memberOf` handle must have structure type");
        throw new BlockEarlyTermination(unreachable());
    }

    @Override
    public ValueHandle elementOf(ValueHandle array, Value index) {
        if (array.getPointeeType() instanceof ArrayType || array.getPointeeType() instanceof ArrayObjectType) {
            if (index.getType() instanceof UnsignedIntegerType uit) {
                // try to extend it
                Value extended = tryExtend(index, uit);
                if (extended != null) {
                    index = extended;
                } else {
                    ctxt.error(getLocation(), "`elementOf` index must be signed");
                }
                // recoverable
            }
            return super.elementOf(array, index);
        }
        ctxt.error(getLocation(), "`elementOf` handle must have array type");
        throw new BlockEarlyTermination(unreachable());
    }

    @Override
    public ValueHandle pointerHandle(Value pointer, Value offsetValue) {
        if (pointer.getType() instanceof PointerType) {
            if (offsetValue.getType() instanceof UnsignedIntegerType uit) {
                // try to extend it
                Value extended = tryExtend(offsetValue, uit);
                if (extended != null) {
                    offsetValue = extended;
                } else {
                    ctxt.error(getLocation(), "`pointerHandle` offset must be signed");
                }
                // recoverable
            }
            return super.pointerHandle(pointer, offsetValue);
        }
        ctxt.error(getLocation(), "`pointerHandle` value must have pointer type");
        throw new BlockEarlyTermination(unreachable());
    }

    @Override
    public Value bitCast(Value value, WordType toType) {
        if (value.getType() instanceof ReferenceType && toType instanceof PointerType) {
            ctxt.error(getLocation(), "Cannot bitcast references to pointers");
        } else if (value.getType() instanceof PointerType && toType instanceof ReferenceType) {
            ctxt.error(getLocation(), "Cannot bitcast pointers to references");
        } else if (value.getType().getSize() != toType.getSize()) {
            ctxt.error(getLocation(), "Cannot bitcast between differently-sized types");
        }
        return super.bitCast(value, toType);
    }

    private Value tryExtend(final Value unsignedValue, final UnsignedIntegerType inputType) {
        final BasicBlockBuilder fb = getFirstBuilder();
        final TypeSystem ts = ctxt.getTypeSystem();
        if (inputType.getMinBits() < 32) {
            return fb.extend(unsignedValue, ts.getSignedInteger32Type());
        } else if (inputType.getMinBits() < 64) {
            return fb.extend(unsignedValue, ts.getSignedInteger64Type());
        } else if (unsignedValue.isDefLe(ctxt.getLiteralFactory().literalOf(ts.getSignedInteger64Type().getMaxValue()))) {
            return fb.bitCast(unsignedValue, ts.getSignedInteger64Type());
        } else {
            // cannot work out a safe conversion
            return null;
        }
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        return super.call(check(target), arguments);
    }

    @Override
    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        return super.callNoSideEffects(check(target), arguments);
    }

    @Override
    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        return super.callNoReturn(check(target), arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(check(target), arguments, catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        return super.tailCall(check(target), arguments);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(check(target), arguments, catchLabel, resumeLabel, targetArguments);
    }

    private ValueHandle check(ValueHandle handle) {
        if (getCurrentElement().hasAllModifiersOf(ClassFile.I_ACC_NO_SAFEPOINTS)) {
            ExecutableElement target;
            // not an exhaustive check but good enough for detecting common errors
            if (handle instanceof InstanceMethodElementHandle h) {
                target = h.getElement();
            } else if (handle instanceof StaticMethodElementHandle h) {
                target = h.getElement();
            } else if (handle instanceof FunctionElementHandle h) {
                target = h.getElement();
            } else {
                return handle;
            }
            if (target.hasNoModifiersOf(ClassFile.I_ACC_NO_SAFEPOINTS)) {
                getContext().error(getLocation(), "This method may not call methods or functions that are not marked as no-safepoint");
            }
        }
        return handle;
    }
}
