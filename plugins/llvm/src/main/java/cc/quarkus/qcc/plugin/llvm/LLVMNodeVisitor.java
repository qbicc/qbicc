package cc.quarkus.qcc.plugin.llvm;

import static cc.quarkus.qcc.machine.llvm.Types.*;
import static cc.quarkus.qcc.machine.llvm.Values.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.graph.Action;
import cc.quarkus.qcc.graph.Add;
import cc.quarkus.qcc.graph.And;
import cc.quarkus.qcc.graph.ArrayElementRead;
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
import cc.quarkus.qcc.graph.MemberPointer;
import cc.quarkus.qcc.graph.InstanceOf;
import cc.quarkus.qcc.graph.Mod;
import cc.quarkus.qcc.graph.Multiply;
import cc.quarkus.qcc.graph.Narrow;
import cc.quarkus.qcc.graph.Neg;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.Or;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.PointerLoad;
import cc.quarkus.qcc.graph.PointerStore;
import cc.quarkus.qcc.graph.Return;
import cc.quarkus.qcc.graph.Select;
import cc.quarkus.qcc.graph.Shl;
import cc.quarkus.qcc.graph.Shr;
import cc.quarkus.qcc.graph.StackAllocation;
import cc.quarkus.qcc.graph.Sub;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.ThisValue;
import cc.quarkus.qcc.graph.Truncate;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueReturn;
import cc.quarkus.qcc.graph.Xor;
import cc.quarkus.qcc.graph.literal.CurrentThreadLiteral;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.llvm.FloatCondition;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.LLBasicBlock;
import cc.quarkus.qcc.machine.llvm.LLValue;
import cc.quarkus.qcc.machine.llvm.op.Call;
import cc.quarkus.qcc.machine.llvm.op.GetElementPtr;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.PointerType;
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
        for (int i = 0, j = 0; i < cnt; i ++) {
            Value value = functionObj.getBody().getParameterValue(i);
            if (value instanceof CurrentThreadLiteral) {
                mappedValues.put(value, func.param(map(value.getType())).name("thr").asValue());
            } else if (value instanceof ThisValue) {
                mappedValues.put(value, func.param(map(value.getType())).name("this").asValue());
            } else {
                mappedValues.put(value, func.param(map(value.getType())).name("p" + j++).asValue());
            }
        }
        func.returns(map(funcType.getReturnType()));
        map(entryBlock);
    }

    // actions

    public Void visit(final Void param, final BlockEntry node) {
        // no operation
        return null;
    }

    public Void visit(final Void param, final PointerStore node) {
        map(node.getDependency());
        LLValue ptrType = map(node.getPointer().getType());
        LLValue value = map(node.getValue());
        LLValue type = map(node.getValue().getType());
        LLValue ptr = map(node.getPointer());
        map(schedule.getBlockForNode(node)).store(ptrType, value, type, ptr);
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

    boolean isPointer(Type type) {
        return type instanceof PointerType;
    }

    public LLValue visit(final Void param, final Add node) {
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        ValueType type = node.getType();
        if (type instanceof PointerType) {
            // actually a GEP calculation
            return processGetElementPtr(target, node).asLocal();
        }
        LLValue inputType = map(type);
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(type) ?
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
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getLeftInput().getType()) ?
               target.fcmp(FloatCondition.oeq, inputType, llvmLeft, llvmRight).asLocal() :
               target.icmp(IntCondition.eq, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpNe node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getLeftInput().getType()) ?
               target.fcmp(FloatCondition.one, inputType, llvmLeft, llvmRight).asLocal() :
               target.icmp(IntCondition.ne, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpLt node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
               target.fcmp(FloatCondition.olt, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(valueType) ?
                      target.icmp(IntCondition.slt, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.ult, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpLe node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
               target.fcmp(FloatCondition.ole, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(valueType) ?
                      target.icmp(IntCondition.sle, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.ule, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpGt node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
               target.fcmp(FloatCondition.ogt, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(valueType) ?
                      target.icmp(IntCondition.sgt, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.ugt, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final CmpGe node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
               target.fcmp(FloatCondition.oge, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(valueType) ?
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
        LLValue result = phi.asLocal();
        mappedValues.put(node, result);
        for (BasicBlock knownBlock : node.incomingBlocks()) {
            Value v = node.getValueForBlock(knownBlock);
            if (v != null) {
                // process dependencies
                phi.item(map(v), map(knownBlock));
            }
        }
        return result;
    }

    public LLValue visit(final Void param, final MemberPointer node) {
        return processGetElementPtr(map(schedule.getBlockForNode(node)), node).asLocal();
    }

    public LLValue visit(final Void param, final PointerLoad node) {
        map(node.getBasicDependency(0));
        LLValue ptr = map(node.getPointer());
        LLValue ptrType = map(node.getPointer().getType());
        LLValue type = map(node.getType());
        return map(schedule.getBlockForNode(node)).load(ptrType, type, ptr).asLocal();
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
        return isPointer(javaInputType) ?
                    target.ptrtoint(inputType, llvmInput, outputType).asLocal() :
                    isPointer(javaOutputType) ?
                    target.inttoptr(inputType, llvmInput, outputType).asLocal() :
                    isFloating(javaInputType) ?
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

    public LLValue visit(final Void param, final Narrow node) {
        return map(node.getInput());
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

    public LLValue visit(final Void param, final StackAllocation node) {
        LLValue pointeeType = map(node.getType().getPointeeType());
        LLValue countType = map(node.getCount().getType());
        LLValue count = map(node.getCount());
        LLValue alignment = map(node.getAlign());
        LLBasicBlock target = map(schedule.getBlockForNode(node));
        return target.alloca(pointeeType).elements(countType, count).align(alignment).asLocal();
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

    // GEP

    private GetElementPtr processGetElementPtr(final LLBasicBlock block, final Value current) {
        if (current instanceof MemberPointer) {
            // a nested pointer within a structure
            MemberPointer memberPointer = (MemberPointer) current;
            return processGetElementPtr(block, memberPointer.getStructPointer()).arg(false, i32, map(memberPointer.getStructType(), memberPointer.getMember()));
        } else if (current instanceof ArrayElementRead) {
            ArrayElementRead read = (ArrayElementRead) current;
            Value array = read.getInstance();
            if (array.getType() instanceof ArrayType) {
                // real array
                return processGetElementPtr(block, array).arg(false, map(read.getIndex().getType()), map(read.getIndex()));
            }
            // else it's some other array access (reference array?)
        } else if (current instanceof Add) {
            // a pointer add; we want the nth one (and terminate)
            Add add = (Add) current;
            Value leftInput = add.getLeftInput();
            if (leftInput.getType() instanceof PointerType) {
                Value index = add.getRightInput();
                return block.getelementptr(block, map(((PointerType) leftInput.getType()).getPointeeType()), map(current)).arg(false, map(index.getType()), map(index));
            } else {
                ctxt.error(Location.builder().setNode(current).build(), "LLVM: Invalid pointer or array expression type (left input of Add must be the pointer or array)");
            }
        }
        // some other kind of pointer; we want the zeroth one (and terminate)
        PointerType pointerType = (PointerType) current.getType();
        return block.getelementptr(map(pointerType.getPointeeType()), map(pointerType), map(current)).arg(false, i32, ZERO);
    }

    // unknown node catch-all methods

    public LLValue visitUnknown(final Void param, final Value node) {
        return node.accept(gen, ctxt);
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

    private LLValue map(CompoundType compoundType, CompoundType.Member member) {
        return gen.map(compoundType, member);
    }
}
