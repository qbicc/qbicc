package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.machine.llvm.LLBasicBlock;
import cc.quarkus.qcc.machine.llvm.FloatCondition;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.LLValue;
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
import cc.quarkus.qcc.machine.llvm.op.Load;
import cc.quarkus.qcc.machine.llvm.op.NuwNswBinary;
import cc.quarkus.qcc.machine.llvm.op.OrderingConstraint;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.machine.llvm.op.Return;
import cc.quarkus.qcc.machine.llvm.op.Select;
import cc.quarkus.qcc.machine.llvm.op.Store;
import cc.quarkus.qcc.machine.llvm.op.YieldingInstruction;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class BasicBlockImpl extends AbstractEmittable implements LLBasicBlock {
    final BasicBlockImpl prev;
    final FunctionDefinitionImpl func;
    final List<AbstractEmittable> phis = new ArrayList<>();
    final List<AbstractEmittable> items = new ArrayList<>();
    AbstractEmittable terminator;
    String name;

    BasicBlockImpl(final BasicBlockImpl prev, final FunctionDefinitionImpl func) {
        this.prev = prev;
        this.func = func;
    }

    public LLBasicBlock name(final String name) {
        this.name = Assert.checkNotNullParam("name", name);
        return this;
    }

    public FunctionDefinition functionDefinition() {
        return func;
    }

    private <I extends AbstractEmittable> I add(I item) {
        items.add(item);
        return item;
    }

    private <I extends AbstractEmittable> I addPhi(I item) {
        phis.add(item);
        return item;
    }

    private void checkTerminated() {
        if (terminator != null) {
            throw new IllegalStateException("Basic block already terminated");
        }
    }

    // not terminator, not starter

    public Phi phi(final LLValue type) {
        Assert.checkNotNullParam("type", type);
        return addPhi(new PhiImpl(this, (AbstractValue) type));
    }

    // terminators

    public Branch br(final LLBasicBlock dest) {
        Assert.checkNotNullParam("dest", dest);
        checkTerminated();
        UnconditionalBranchImpl res = new UnconditionalBranchImpl((BasicBlockImpl) dest);
        terminator = res;
        return res;
    }

    public Branch br(final LLValue cond, final LLBasicBlock ifTrue, final LLBasicBlock ifFalse) {
        Assert.checkNotNullParam("cond", cond);
        Assert.checkNotNullParam("ifTrue", ifTrue);
        Assert.checkNotNullParam("ifFalse", ifFalse);
        checkTerminated();
        ConditionalBranchImpl res = new ConditionalBranchImpl((AbstractValue) cond, (BasicBlockImpl) ifTrue, (BasicBlockImpl) ifFalse);
        terminator = res;
        return res;
    }

    public Return ret() {
        checkTerminated();
        terminator = VoidReturn.INSTANCE;
        return VoidReturn.INSTANCE;
    }

    public Return ret(final LLValue type, final LLValue val) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("val", val);
        checkTerminated();
        ValueReturn valueReturn = new ValueReturn((AbstractValue) type, (AbstractValue) val);
        terminator = valueReturn;
        return valueReturn;
    }

    public void unreachable() {
        checkTerminated();
        terminator = Unreachable.INSTANCE;
    }

    public Call invoke(final LLValue type, final LLValue function, final LLBasicBlock normal, final LLBasicBlock unwind) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("function", function);
        Assert.checkNotNullParam("normal", normal);
        Assert.checkNotNullParam("unwind", unwind);
        checkTerminated();
        InvokeImpl invoke = new InvokeImpl(this, (AbstractValue) type, (AbstractValue) function, (BasicBlockImpl) normal, (BasicBlockImpl) unwind);
        terminator = invoke;
        return invoke;
    }

    // starters

    public Assignment assign(final LLValue value) {
        Assert.checkNotNullParam("value", value);
        return add(new AssignmentImpl(this, (AbstractValue) value));
    }

    public Select select(final LLValue condType, final LLValue cond, final LLValue valueType, final LLValue trueValue, final LLValue falseValue) {
        return add(new SelectImpl(this, (AbstractValue) condType, (AbstractValue) cond, (AbstractValue) valueType, (AbstractValue) trueValue, (AbstractValue) falseValue));
    }

    public NuwNswBinary add(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new AddImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public NuwNswBinary sub(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new SubImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public NuwNswBinary mul(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new MulImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public NuwNswBinary shl(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new ShlImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary udiv(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new UdivImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary sdiv(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new SdivImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary lshr(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new LshrImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary ashr(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new AshrImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary fmul(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new FMulImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary fcmp(final FloatCondition cond, final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("cond", cond);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new FCmpImpl(this, cond, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary fadd(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new FAddImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary fsub(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new FSubImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary fdiv(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new FDivImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathBinary frem(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new FRemImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public FastMathUnary fneg(final LLValue type, final LLValue arg) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg", arg);
        return add(new FNegImpl(this, (AbstractValue) type, (AbstractValue) arg));
    }

    public Binary icmp(final IntCondition cond, final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("cond", cond);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new IcmpImpl(this, cond, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary and(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new AndImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary or(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new OrImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary xor(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new XorImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary urem(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new URemImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary srem(final LLValue type, final LLValue arg1, final LLValue arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        return add(new SRemImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public YieldingInstruction trunc(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return add(new TruncImpl(this, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction ftrunc(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return add(new FTruncImpl(this, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction fpext(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return add(new FPExtImpl(this, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction sext(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return add(new SExtImpl(this, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction zext(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return add(new ZExtImpl(this, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction bitcast(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return add(new BitCastImpl(this, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction fptosi(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return add(new FPToSI(this, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction fptoui(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return add(new FPToUI(this, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction sitofp(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return add(new SIToFP(this, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public YieldingInstruction uitofp(final LLValue type, final LLValue value, final LLValue toType) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("toType", toType);
        return add(new UIToFP(this, (AbstractValue) type, (AbstractValue) value, (AbstractValue) toType));
    }

    public Call call(final LLValue type, final LLValue function) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("function", function);
        return add(new CallImpl(this, (AbstractValue) type, (AbstractValue) function));
    }

    public Load load(final LLValue type, final LLValue pointeeType, final LLValue pointer) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("pointeeType", pointeeType);
        Assert.checkNotNullParam("pointer", pointer);
        return add(new LoadImpl(this, (AbstractValue) type, (AbstractValue) pointeeType, (AbstractValue) pointer));
    }

    public Store store(final LLValue type, final LLValue value, final LLValue pointeeType, final LLValue pointer) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("pointeeType", pointeeType);
        Assert.checkNotNullParam("pointer", pointer);
        return add(new StoreImpl((AbstractValue) type, (AbstractValue) value, (AbstractValue) pointeeType, (AbstractValue) pointer));
    }

    public Fence fence(final OrderingConstraint ordering) {
        Assert.checkNotNullParam("ordering", ordering);
        return add(new FenceImpl(ordering));
    }

    public AtomicRmwInstruction atomicrmw() {
        throw Assert.unsupported();
    }

    public GetElementPtr getelementptr(final LLValue type, final LLValue ptrType, final LLValue pointer) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("ptrType", ptrType);
        Assert.checkNotNullParam("pointer", pointer);
        return add(new GetElementPtrImpl(this, (AbstractValue) type, (AbstractValue) ptrType, (AbstractValue) pointer));
    }

    public Alloca alloca(final LLValue type) {
        Assert.checkNotNullParam("type", type);
        return add(new AllocaImpl(this, (AbstractValue) type));
    }

    public LLBasicBlock createBlock() {
        return func.createBlock();
    }

    @SuppressWarnings("UnusedReturnValue")
    Appendable appendAsBlockTo(final Appendable target) throws IOException {
        final BasicBlockImpl prev = this.prev;
        if (prev != null) {
            prev.appendAsBlockTo(target);
        }
        if (phis.isEmpty() && items.isEmpty() && terminator == null) {
            // no block;
            return target;
        }
        if (terminator == null) {
            throw new IllegalStateException("Basic block not terminated");
        }
        if (this != func.rootBlock) {
            if (name == null) {
                func.assignName(this);
            }
            target.append(name).append(':').append(System.lineSeparator());
        }
        for (List<AbstractEmittable> list : List.of(phis, items)) {
            for (AbstractEmittable item : list) {
                target.append("  ");
                item.appendTo(target);
                target.append(System.lineSeparator());
            }
        }
        target.append("  ");
        terminator.appendTo(target);
        target.append(System.lineSeparator());
        return target;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('%');
        if (this == func.rootBlock) {
            target.append('0');
        } else {
            if (name == null) {
                func.assignName(this);
            }
            target.append(name);
        }
        return target;
    }
}
