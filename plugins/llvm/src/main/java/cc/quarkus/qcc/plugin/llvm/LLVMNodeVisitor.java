package cc.quarkus.qcc.plugin.llvm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.Action;
import cc.quarkus.qcc.graph.Add;
import cc.quarkus.qcc.graph.And;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BitCast;
import cc.quarkus.qcc.graph.BlockEntry;
import cc.quarkus.qcc.graph.Catch;
import cc.quarkus.qcc.graph.CmpEq;
import cc.quarkus.qcc.graph.CmpGe;
import cc.quarkus.qcc.graph.CmpGt;
import cc.quarkus.qcc.graph.CmpLe;
import cc.quarkus.qcc.graph.CmpLt;
import cc.quarkus.qcc.graph.CmpNe;
import cc.quarkus.qcc.graph.Convert;
import cc.quarkus.qcc.graph.Div;
import cc.quarkus.qcc.graph.Extend;
import cc.quarkus.qcc.graph.FunctionCall;
import cc.quarkus.qcc.graph.Goto;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.Mod;
import cc.quarkus.qcc.graph.Multiply;
import cc.quarkus.qcc.graph.Neg;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.Or;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Return;
import cc.quarkus.qcc.graph.Select;
import cc.quarkus.qcc.graph.Shl;
import cc.quarkus.qcc.graph.Shr;
import cc.quarkus.qcc.graph.Sub;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.Truncate;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueReturn;
import cc.quarkus.qcc.graph.Xor;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.llvm.FloatCondition;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.LLBasicBlock;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.impl.LLVM;
import cc.quarkus.qcc.machine.llvm.op.Call;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.MethodBody;

final class LLVMNodeVisitor implements NodeVisitor<Void, LLValue, Void, Void> {
    final CompilationContext ctxt;
    final LLVMGenerator gen;
    final Schedule schedule;
    final Function functionObj;
    final FunctionDefinition func;
    final BasicBlock entryBlock;
    final Set<Action> visitedActions = new HashSet<>();
    final Map<Value, LLValue> mappedValues = new HashMap<>();
    final Map<BasicBlock, LLBasicBlock> mappedBlocks = new HashMap<>();
    final MethodBody methodBody;

    LLVMNodeVisitor(final CompilationContext ctxt, final LLVMGenerator gen, final Schedule schedule, final Function functionObj, final FunctionDefinition func) {
        this.ctxt = ctxt;
        this.gen = gen;
        this.schedule = schedule;
        this.functionObj = functionObj;
        this.func = func;
        this.methodBody = functionObj.getBody();
        entryBlock = methodBody.getEntryBlock();
    }

    // begin

    public void execute() {
        FunctionType funcType = functionObj.getType();
        int cnt = methodBody.getParameterCount();
        ValueType type = funcType.getParameterType(0);
        // it should be java.lang.Thread
        mappedValues.put(ctxt.getCurrentThreadValue(), func.param(map(type)).name("thr").asValue());
        Value thisValue = methodBody.getThisValue();
        if (thisValue != null) {
            mappedValues.put(thisValue, func.param(map(thisValue.getType())).name("this").asValue());
        }
        for (int i = 0; i < cnt; i ++) {
            Value value = functionObj.getBody().getParameterValue(i);
            mappedValues.put(value, func.param(map(value.getType())).name("p" + i).asValue());
        }
        func.returns(map(funcType.getReturnType()));
        map(entryBlock);
    }

    // actions

    public Void visit(final Void param, final BlockEntry node) {
        // no operation
        return null;
    }

    // terminators

    public Void visit(final Void param, final Goto node) {
        map(node.getBasicDependency(0));
        LLBasicBlock block = map(schedule.getBlockForNode(node));
        block.br(map(node.getResumeTarget()));
        return null;
    }

    public Void visit(final Void param, final If node) {
        map(node.getBasicDependency(0));
        LLBasicBlock block = map(schedule.getBlockForNode(node));
        block.br(map(node.getCondition()), map(node.getTrueBranch()), map(node.getFalseBranch()));
        return null;
    }

    public Void visit(final Void param, final Return node) {
        map(node.getBasicDependency(0));
        LLBasicBlock block = map(schedule.getBlockForNode(node));
        block.ret();
        return null;
    }

    public Void visit(final Void param, final ValueReturn node) {
        map(node.getBasicDependency(0));
        LLBasicBlock block = map(schedule.getBlockForNode(node));
        block.ret(map(node.getReturnValue().getType()), map(node.getReturnValue()));
        return null;
    }

    // values

    boolean isFloating(Type type) {
        return type instanceof FloatType;
    }

    boolean isSigned(Type type) {
        return type instanceof SignedIntegerType;
    }

    public LLValue visit(final Void param, final Add node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fadd(inputType, llvmLeft, llvmRight).asLocal() :
               target.add(inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final And node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return map(schedule.getBlockForNode(node)).and(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Or node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return map(schedule.getBlockForNode(node)).or(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Xor node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return map(schedule.getBlockForNode(node)).xor(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Multiply node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fmul(inputType, llvmLeft, llvmRight).asLocal() :
               target.mul(inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpEq node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.oeq, inputType, llvmLeft, llvmRight).asLocal() :
               target.icmp(IntCondition.eq, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpNe node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.one, inputType, llvmLeft, llvmRight).asLocal() :
               target.icmp(IntCondition.ne, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpLt node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.olt, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.icmp(IntCondition.slt, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.ult, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpLe node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.ole, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.icmp(IntCondition.sle, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.ule, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpGt node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.ogt, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.icmp(IntCondition.sgt, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.ugt, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpGe node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.oge, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.icmp(IntCondition.sge, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.uge, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Select node) {
        Value trueValue = node.getTrueValue();
        LLValue inputType = map(trueValue.getType());
        Value falseValue = node.getFalseValue();
        return map(schedule.getBlockForNode(node)).select(map(node.getCondition().getType()), map(node.getCondition()), inputType, map(trueValue), map(falseValue)).asLocal();
    }

    public LLValue visit(final Void param, final Catch node) {
        // todo: landingpad
        return null;
    }

    public LLValue visit(final Void param, final PhiValue node) {
        Phi phi = map(schedule.getBlockForNode(node)).phi(map(node.getType()));
        mappedValues.put(node, phi.asLocal());
        for (BasicBlock knownBlock : node.incomingBlocks()) {
            Value v = node.getValueForBlock(knownBlock);
            if (v != null) {
                // process dependencies
                v.accept(this, param);
                phi.item(map(v), map(knownBlock));
            }
        }
        return phi.asLocal();
    }

    public LLValue visit(final Void param, final Neg node) {
        Type javaInputType = node.getInput().getType();
        LLValue inputType = map(javaInputType);
        LLValue llvmInput = map(node.getInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return target.fneg(inputType, llvmInput).asLocal();
    }

    public LLValue visit(final Void param, final Shr node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isSigned(node.getType()) ?
               target.ashr(inputType, llvmLeft, llvmRight).asLocal() :
               target.lshr(inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Shl node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return map(schedule.getBlockForNode(node)).shl(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Sub node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return map(schedule.getBlockForNode(node)).sub(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Div node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fdiv(inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.sdiv(inputType, llvmLeft, llvmRight).asLocal() :
                      target.udiv(inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Mod node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.frem(inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.srem(inputType, llvmLeft, llvmRight).asLocal() :
                      target.urem(inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final BitCast node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        LLValue llvmInput = map(node.getInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return target.bitcast(inputType, llvmInput, outputType).asLocal();
    }

    public LLValue visit(final Void param, final Convert node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        LLValue llvmInput = map(node.getInput());
        if (inputType.equals(outputType)) {
            return llvmInput;
        }
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(javaInputType) ?
                    isSigned(javaOutputType) ?
                    target.fptosi(inputType, llvmInput, outputType).asLocal() :
                    target.fptoui(inputType, llvmInput, outputType).asLocal() :
                    isSigned(javaInputType) ?
                    target.sitofp(inputType, llvmInput, outputType).asLocal() :
                    target.uitofp(inputType, llvmInput, outputType).asLocal();
    }

    public LLValue visit(final Void param, final Extend node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        LLValue llvmInput = map(node.getInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(javaInputType) ?
               target.fpext(inputType, llvmInput, outputType).asLocal() :
                    isSigned(javaInputType) ?
                    target.sext(inputType, llvmInput, outputType).asLocal() :
                    target.zext(inputType, llvmInput, outputType).asLocal();
    }

    public LLValue visit(final Void param, final Truncate node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        LLValue llvmInput = map(node.getInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(javaInputType) ?
               target.ftrunc(inputType, llvmInput, outputType).asLocal() :
               target.trunc(inputType, llvmInput, outputType).asLocal();
    }

    // calls

    public LLValue visit(final Void param, final FunctionCall node) {
        map(node.getBasicDependency(0));
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        FunctionType functionType = node.getFunctionType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        LLValue llTarget = map(node.getCallTarget());
        // two scans - once to populate the maps, and then once to emit the call in the right order
        for (int i = 0; i < arguments.size(); i++) {
            map(functionType.getParameterType(i));
            map(arguments.get(i));
        }
        Call call = target.call(llType, llTarget);
        for (int i = 0; i < arguments.size(); i++) {
            call.arg(map(functionType.getParameterType(i)), map(arguments.get(i)));
        }
        return call.asLocal();
    }

    // literals

    public LLValue visit(final Void param, final IntegerLiteral node) {
        return Values.intConstant(node.longValue());
    }

    public LLValue visit(final Void param, final SymbolLiteral node) {
        return Values.global(node.getName());
    }

    // unknown node catch-all methods

    public LLValue visitUnknown(final Void param, final Value node) {
        ctxt.error(functionObj.getOriginalElement(), node, "llvm: Unrecognized value %s", node.getClass());
        return LLVM.FALSE;
    }

    public Void visitUnknown(final Void param, final Action node) {
        ctxt.error(functionObj.getOriginalElement(), node, "llvm: Unrecognized action %s", node.getClass());
        return null;
    }

    public Void visitUnknown(final Void param, final Terminator node) {
        ctxt.error(functionObj.getOriginalElement(), node, "llvm: Unrecognized terminator %s", node.getClass());
        return null;
    }

    // mapping

    private LLBasicBlock map(BasicBlock block) {
        LLBasicBlock mapped = mappedBlocks.get(block);
        if (mapped != null) {
            return mapped;
        }
        mapped = func.createBlock();
        mappedBlocks.put(block, mapped);
        block.getTerminator().accept(this, null);
        return mapped;
    }

    private LLValue map(Type type) {
        return gen.map(type);
    }

    private void map(Action action) {
        if (visitedActions.add(action)) {
            action.accept(this, null);
        }
    }

    private LLValue map(Value value) {
        LLValue mapped = mappedValues.get(value);
        if (mapped != null) {
            return mapped;
        }
        mapped = value.accept(this, null);
        mappedValues.put(value, mapped);
        return mapped;
    }

    private void map(Node unknown) {
        if (unknown instanceof Action) {
            map((Action) unknown);
        } else if (unknown instanceof Value) {
            map((Value) unknown);
        } else {
            throw new IllegalStateException();
        }
    }
}
