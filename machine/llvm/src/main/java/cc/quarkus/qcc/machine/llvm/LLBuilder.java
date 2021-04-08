package cc.quarkus.qcc.machine.llvm;

import cc.quarkus.qcc.machine.llvm.impl.LLVM;
import cc.quarkus.qcc.machine.llvm.op.Alloca;
import cc.quarkus.qcc.machine.llvm.op.Assignment;
import cc.quarkus.qcc.machine.llvm.op.AtomicRmwInstruction;
import cc.quarkus.qcc.machine.llvm.op.Binary;
import cc.quarkus.qcc.machine.llvm.op.Branch;
import cc.quarkus.qcc.machine.llvm.op.Call;
import cc.quarkus.qcc.machine.llvm.op.ExactBinary;
import cc.quarkus.qcc.machine.llvm.op.ExtractValue;
import cc.quarkus.qcc.machine.llvm.op.FastMathBinary;
import cc.quarkus.qcc.machine.llvm.op.FastMathUnary;
import cc.quarkus.qcc.machine.llvm.op.Fence;
import cc.quarkus.qcc.machine.llvm.op.GetElementPtr;
import cc.quarkus.qcc.machine.llvm.op.Instruction;
import cc.quarkus.qcc.machine.llvm.op.LandingPad;
import cc.quarkus.qcc.machine.llvm.op.Load;
import cc.quarkus.qcc.machine.llvm.op.NuwNswBinary;
import cc.quarkus.qcc.machine.llvm.op.OrderingConstraint;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.machine.llvm.op.Return;
import cc.quarkus.qcc.machine.llvm.op.Select;
import cc.quarkus.qcc.machine.llvm.op.Store;
import cc.quarkus.qcc.machine.llvm.op.Switch;
import cc.quarkus.qcc.machine.llvm.op.YieldingInstruction;

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

    Alloca alloca(LLValue type);

    static LLBuilder newBuilder(LLBasicBlock block) {
        return LLVM.newBuilder(block);
    }
}
