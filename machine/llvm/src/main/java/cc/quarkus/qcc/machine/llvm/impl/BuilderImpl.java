package cc.quarkus.qcc.machine.llvm.impl;

import cc.quarkus.qcc.machine.llvm.FloatCondition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.LLBasicBlock;
import cc.quarkus.qcc.machine.llvm.LLBuilder;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Metable;
import cc.quarkus.qcc.machine.llvm.op.Alloca;
import cc.quarkus.qcc.machine.llvm.op.Assignment;
import cc.quarkus.qcc.machine.llvm.op.AtomicRmwInstruction;
import cc.quarkus.qcc.machine.llvm.op.Binary;
import cc.quarkus.qcc.machine.llvm.op.Branch;
import cc.quarkus.qcc.machine.llvm.op.Call;
import cc.quarkus.qcc.machine.llvm.op.ExactBinary;
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
import io.smallrye.common.constraint.Assert;

final class BuilderImpl implements LLBuilder {
    BasicBlockImpl block;
    AbstractValue debugLocation;

    BuilderImpl(BasicBlockImpl block) {
        this.block = block;
    }

    public LLValue getDebugLocation() {
        return debugLocation;
    }

    public LLValue setDebugLocation(LLValue debugLocation) {
        AbstractValue oldDebugLocation = this.debugLocation;
        this.debugLocation = (AbstractValue) debugLocation;
        return oldDebugLocation;
    }

    public LLBasicBlock getCurrentBlock() {
        return block;
    }

    public LLBasicBlock moveToBlock(LLBasicBlock block) {
        Assert.checkNotNullParam("block", block);

        BasicBlockImpl oldBlock = this.block;
        this.block = (BasicBlockImpl) block;
        return oldBlock;
    }

    private <I extends Metable> I attachDbg(I instr) {
        if (debugLocation != null)
            instr.meta("dbg", debugLocation);
        return instr;
    }

    private <I extends AbstractEmittable & Metable> I append(I instr) {
        block.items.add(attachDbg(instr));
        return instr;
    }

    private <I extends AbstractEmittable & Metable> I appendTerminator(I instr) {
        if (block.terminator != null)
            throw new IllegalStateException("Basic block already terminated");

        block.terminator = attachDbg(instr);
        return instr;
    }

    private <I extends AbstractEmittable & Metable> I appendPhi(I instr) {
        block.phis.add(attachDbg(instr));
        return instr;
    }

    // not terminator, not starter

    public Phi phi(final LLValue type) {
        Assert.checkNotNullParam("type", type);
        return appendPhi(new PhiImpl(block, (AbstractValue) type));
    }

    // terminators

    public Branch br(final LLBasicBlock dest) {
        Assert.checkNotNullParam("dest", dest);
        return appendTerminator(new UnconditionalBranchImpl((BasicBlockImpl) dest));
    }

    public Branch br(final LLValue cond, final LLBasicBlock ifTrue, final LLBasicBlock ifFalse) {
        Assert.checkNotNullParam("cond", cond);
        Assert.checkNotNullParam("ifTrue", ifTrue);
        Assert.checkNotNullParam("ifFalse", ifFalse);
        return appendTerminator(new ConditionalBranchImpl((AbstractValue) cond, (BasicBlockImpl) ifTrue, (BasicBlockImpl) ifFalse));
    }

    public Return ret() {
        return appendTerminator(VoidReturn.INSTANCE);
    }

    public Return ret(final LLValue type, final LLValue val) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("val", val);
        return appendTerminator(new ValueReturn((AbstractValue) type, (AbstractValue) val));
    }

    public Switch switch_(final LLValue type, final LLValue value, final LLBasicBlock defaultTarget) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("defaultTarget", defaultTarget);
        return appendTerminator(new SwitchImpl((AbstractValue) type, (AbstractValue) value, (BasicBlockImpl) defaultTarget));
    }

    public Instruction unreachable() {
        return appendTerminator(Unreachable.INSTANCE);
    }

    public Call invoke(final LLValue type, final LLValue function, final LLBasicBlock normal, final LLBasicBlock unwind) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("function", function);
        Assert.checkNotNullParam("normal", normal);
        Assert.checkNotNullParam("unwind", unwind);
        return appendTerminator(new InvokeImpl(block, (AbstractValue) type, (AbstractValue) function, (BasicBlockImpl) normal, (BasicBlockImpl) unwind));
    }

    // starters

    public LandingPad landingpad(final LLValue resultType) {
        Assert.checkNotNullParam("resultType", resultType);
        return append(new LandingPadImpl(block, (AbstractValue) resultType));
    }

    public Assignment assign(final LLValue value) {
        Assert.checkNotNullParam("value", value);
        return append(new AssignmentImpl(block, (AbstractValue) value));
    }

    public Select select(final LLValue condType, final LLValue cond, final LLValue valueType, final LLValue trueValue, final LLValue falseValue) {
        return append(new SelectImpl(block, (AbstractValue) condType, (AbstractValue) cond, (AbstractValue) valueType, (AbstractValue) trueValue, (AbstractValue) falseValue));
    }

    public NuwNswBinary add(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new AddImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public NuwNswBinary sub(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new SubImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public NuwNswBinary mul(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new MulImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public NuwNswBinary shl(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new ShlImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary udiv(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new UdivImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary sdiv(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new SdivImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary lshr(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new LshrImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary ashr(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new AshrImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary fmul(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new FMulImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary fcmp(final FloatCondition cond, final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("cond", cond);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new FCmpImpl(block, cond, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary fadd(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new FAddImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary fsub(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new FSubImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary fdiv(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new FDivImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary frem(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new FRemImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathUnary fneg(final LLValue type, final LLValue arg) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg", arg);
        return append(new FNegImpl(block, (AbstractValue) type, (AbstractValue) arg));
    }

    public Binary icmp(final IntCondition cond, final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("cond", cond);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new IcmpImpl(block, cond, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary and(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new AndImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary or(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new OrImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary xor(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new XorImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary urem(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new URemImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary srem(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return append(new SRemImpl(block, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public YieldingInstruction trunc(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new TruncImpl(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction ftrunc(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new FTruncImpl(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction fpext(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new FPExtImpl(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction sext(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new SExtImpl(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction zext(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new ZExtImpl(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction bitcast(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new BitCastImpl(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction fptosi(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new FPToSI(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction fptoui(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new FPToUI(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction sitofp(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new SIToFP(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction uitofp(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new UIToFP(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction ptrtoint(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new PtrToInt(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction inttoptr(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return append(new IntToPtr(block, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public Call call(final LLValue type, final LLValue function) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("function", function);
        return append(new CallImpl(block, (AbstractValue) type, (AbstractValue) function));
    }

    public Load load(final LLValue type, final LLValue pointeeType, final LLValue pointer) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("pointeeType", pointeeType);
        Assert.checkNotNullParam("pointer", pointer);
        return append(new LoadImpl(block, (AbstractValue) type, (AbstractValue) pointeeType, (AbstractValue) pointer));
    }

    public Store store(final LLValue type, final LLValue value, final LLValue pointeeType, final LLValue pointer) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("pointeeType", pointeeType);
        Assert.checkNotNullParam("pointer", pointer);
        return append(new StoreImpl((AbstractValue) type, (AbstractValue) value, (AbstractValue) pointeeType, (AbstractValue) pointer));
    }

    public Fence fence(final OrderingConstraint ordering) {
        Assert.checkNotNullParam("ordering", ordering);
        return append(new FenceImpl(ordering));
    }

    public AtomicRmwInstruction atomicrmw() {
        throw Assert.unsupported();
    }

    public GetElementPtr getelementptr(final LLValue type, final LLValue ptrType, final LLValue pointer) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("ptrType", ptrType);
        Assert.checkNotNullParam("pointer", pointer);
        return append(new GetElementPtrImpl(block, (AbstractValue) type, (AbstractValue) ptrType, (AbstractValue) pointer));
    }

    public Alloca alloca(final LLValue type) {
        Assert.checkNotNullParam("type", type);
        return append(new AllocaImpl(block, (AbstractValue) type));
    }
}
