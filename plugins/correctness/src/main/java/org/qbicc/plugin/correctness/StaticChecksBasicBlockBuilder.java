package org.qbicc.plugin.correctness;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.ExecutableLiteral;
import org.qbicc.graph.literal.InitializerLiteral;
import org.qbicc.graph.literal.MethodLiteral;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.StructType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnionType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InstanceFieldElement;

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
    public Value load(Value pointer, ReadAccessMode accessMode) {
        return super.load(checkTargetType(pointer), accessMode);
    }

    @Override
    public Value loadLength(Value arrayPointer) {
        return super.loadLength(checkTargetType(arrayPointer));
    }

    @Override
    public Node store(Value handle, Value value, WriteAccessMode accessMode) {
        return super.store(checkTargetType(handle), value, accessMode);
    }

    @Override
    public Value cmpAndSwap(Value target, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        return super.cmpAndSwap(checkTargetType(target), expect, update, readMode, writeMode, strength);
    }

    @Override
    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return super.readModifyWrite(checkTargetType(pointer), op, update, readMode, writeMode);
    }

    @Override
    public Value instanceFieldOf(Value instancePointer, InstanceFieldElement field) {
        return super.instanceFieldOf(checkTargetType(instancePointer), field);
    }

    @Override
    public Value byteOffsetPointer(Value base, Value offset, ValueType outputType) {
        return super.byteOffsetPointer(checkTargetType(base, true), offset, outputType);
    }

    @Override
    public Value deref(Value pointer) {
        return super.deref(checkTargetType(pointer));
    }

    private Value checkTargetType(Value target) {
        return checkTargetType(target, false);
    }

    private Value checkTargetType(Value target, boolean allowVoid) {
        if (! (target.getType() instanceof PointerType pt)) {
            ctxt.error(getLocation(), "Target must be of pointer type");
            return getLiteralFactory().nullLiteralOfType(getTypeSystem().getVoidType().getPointer());
        }
        if (! allowVoid && pt.getPointeeType() instanceof VoidType) {
            ctxt.error(getLocation(), "Target must not be pointer to void");
        }
        return target;
    }

    @Override
    public Value memberOf(Value structPointer, StructType.Member member) {
        if (checkTargetType(structPointer).getType(PointerType.class).getPointeeType() instanceof StructType) {
            return super.memberOf(structPointer, member);
        }
        ctxt.error(getLocation(), "`memberOf` handle must have structure type");
        throw new BlockEarlyTermination(unreachable());
    }

    @Override
    public Value memberOfUnion(Value unionPointer, UnionType.Member member) {
        if (checkTargetType(unionPointer).getType(PointerType.class).getPointeeType() instanceof UnionType) {
            return super.memberOfUnion(unionPointer, member);
        }
        ctxt.error(getLocation(), "`memberOfUnion` handle must have union type");
        throw new BlockEarlyTermination(unreachable());
    }

    @Override
    public Value elementOf(Value arrayPointer, Value index) {
        Value checkedPointer = checkTargetType(arrayPointer);
        if (checkedPointer.getPointeeType() instanceof ArrayType || checkedPointer.getPointeeType() instanceof ArrayObjectType) {
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
            return super.elementOf(arrayPointer, index);
        }
        ctxt.error(getLocation(), "`elementOf` handle must have array type");
        throw new BlockEarlyTermination(unreachable());
    }

    @Override
    public Value offsetPointer(Value basePointer, Value offset) {
        if (offset.getType() instanceof UnsignedIntegerType uit) {
            // try to extend it
            Value extended = tryExtend(offset, uit);
            if (extended != null) {
                offset = extended;
            } else {
                ctxt.error(getLocation(), "`offsetHandle` offset must be signed");
            }
        }
        return super.offsetPointer(checkTargetType(basePointer, true), offset);
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
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.call(check(targetPtr), receiver, arguments);
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoSideEffects(check(targetPtr), receiver, arguments);
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoReturn(check(targetPtr), receiver, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(check(targetPtr), receiver, arguments, catchLabel, targetArguments);
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.tailCall(check(targetPtr), receiver, arguments);
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(check(targetPtr), receiver, arguments, catchLabel, resumeLabel, targetArguments);
    }

    private Value check(Value targetPtr) {
        checkTargetType(targetPtr);
        return targetPtr;
    }
}
