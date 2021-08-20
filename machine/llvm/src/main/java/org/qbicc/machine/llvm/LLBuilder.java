package org.qbicc.machine.llvm;

import org.qbicc.machine.llvm.impl.LLVM;
import org.qbicc.machine.llvm.op.Alloca;
import org.qbicc.machine.llvm.op.Assignment;
import org.qbicc.machine.llvm.op.AtomicRmwInstruction;
import org.qbicc.machine.llvm.op.Binary;
import org.qbicc.machine.llvm.op.Branch;
import org.qbicc.machine.llvm.op.Call;
import org.qbicc.machine.llvm.op.CmpAndSwap;
import org.qbicc.machine.llvm.op.ExactBinary;
import org.qbicc.machine.llvm.op.ExtractValue;
import org.qbicc.machine.llvm.op.FastMathBinary;
import org.qbicc.machine.llvm.op.FastMathUnary;
import org.qbicc.machine.llvm.op.Fence;
import org.qbicc.machine.llvm.op.GetElementPtr;
import org.qbicc.machine.llvm.op.Instruction;
import org.qbicc.machine.llvm.op.LandingPad;
import org.qbicc.machine.llvm.op.Load;
import org.qbicc.machine.llvm.op.NuwNswBinary;
import org.qbicc.machine.llvm.op.OrderingConstraint;
import org.qbicc.machine.llvm.op.Phi;
import org.qbicc.machine.llvm.op.Return;
import org.qbicc.machine.llvm.op.Select;
import org.qbicc.machine.llvm.op.Store;
import org.qbicc.machine.llvm.op.Switch;
import org.qbicc.machine.llvm.op.YieldingInstruction;

public interface LLBuilder {
    LLValue getDebugLocation();
    LLValue setDebugLocation(LLValue debugLocation);

    LLBasicBlock getCurrentBlock();
    LLBasicBlock moveToBlock(LLBasicBlock block);

    // not starter, not terminator

    Phi phi(LLValue type);

    // terminators

    Branch br(LLBasicBlock dest);

    Branch br(LLValue cond, LLBasicBlock ifTrue, LLBasicBlock ifFalse);

    Return ret();

    Return ret(LLValue type, LLValue val);

    Switch switch_(LLValue type, LLValue value, LLBasicBlock defaultTarget);

    Instruction unreachable();

    // starters

    LandingPad landingpad(LLValue resultType);

    Assignment assign(LLValue value);

    Select select(LLValue condType, LLValue cond, LLValue valueType, LLValue trueValue, LLValue falseValue);

    NuwNswBinary add(LLValue type, LLValue arg1, LLValue arg2);

    NuwNswBinary sub(LLValue type, LLValue arg1, LLValue arg2);

    NuwNswBinary mul(LLValue type, LLValue arg1, LLValue arg2);

    NuwNswBinary shl(LLValue type, LLValue arg1, LLValue arg2);

    ExactBinary udiv(LLValue type, LLValue arg1, LLValue arg2);

    ExactBinary sdiv(LLValue type, LLValue arg1, LLValue arg2);

    ExactBinary lshr(LLValue type, LLValue arg1, LLValue arg2);

    ExactBinary ashr(LLValue type, LLValue arg1, LLValue arg2);

    FastMathBinary fcmp(FloatCondition cond, LLValue type, LLValue arg1, LLValue arg2);

    FastMathBinary fadd(LLValue type, LLValue arg1, LLValue arg2);

    FastMathBinary fsub(LLValue type, LLValue arg1, LLValue arg2);

    FastMathBinary fmul(LLValue type, LLValue arg1, LLValue arg2);

    FastMathBinary fdiv(LLValue type, LLValue arg1, LLValue arg2);

    FastMathBinary frem(LLValue type, LLValue arg1, LLValue arg2);

    FastMathUnary fneg(LLValue type, LLValue arg);

    Binary icmp(IntCondition cond, LLValue type, LLValue arg1, LLValue arg2);

    Binary and(LLValue type, LLValue arg1, LLValue arg2);

    Binary or(LLValue type, LLValue arg1, LLValue arg2);

    Binary xor(LLValue type, LLValue arg1, LLValue arg2);

    Binary urem(LLValue type, LLValue arg1, LLValue arg2);

    Binary srem(LLValue type, LLValue arg1, LLValue arg2);

    YieldingInstruction trunc(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction ftrunc(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction fpext(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction sext(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction zext(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction bitcast(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction addrspacecast(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction fptosi(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction fptoui(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction sitofp(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction uitofp(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction ptrtoint(LLValue type, LLValue value, LLValue toType);

    YieldingInstruction inttoptr(LLValue type, LLValue value, LLValue toType);

    Call call(LLValue type, LLValue function);

    Call invoke(LLValue type, LLValue function, LLBasicBlock normal, LLBasicBlock unwind);

    Load load(LLValue type, LLValue pointeeType, LLValue pointer);

    Store store(LLValue type, LLValue value, LLValue pointeeType, LLValue pointer);

    Fence fence(OrderingConstraint ordering);

    AtomicRmwInstruction atomicrmw();

    GetElementPtr getelementptr(LLValue type, LLValue ptrType, LLValue pointer);

    ExtractValue extractvalue(LLValue aggregateType, LLValue aggregate);

    ExtractValue insertvalue(LLValue aggregateType, LLValue aggregate, LLValue insertType, LLValue insert);

    Alloca alloca(LLValue type);

    CmpAndSwap cmpAndSwap(final LLValue pointerType, final LLValue type, final LLValue pointer, final LLValue expect, final LLValue update,
                          final OrderingConstraint successOrdering, final OrderingConstraint failureOrdering);

    static LLBuilder newBuilder(LLBasicBlock block) {
        return LLVM.newBuilder(block);
    }
}
