package org.qbicc.plugin.llvm;

import static org.qbicc.machine.llvm.Types.*;
import static org.qbicc.machine.llvm.Values.ZERO;
import static org.qbicc.machine.llvm.Values.diExpression;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.graph.Action;
import org.qbicc.graph.Add;
import org.qbicc.graph.AddressOf;
import org.qbicc.graph.And;
import org.qbicc.graph.AsmHandle;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.CallNoReturn;
import org.qbicc.graph.CallNoSideEffects;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.Cmp;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.CmpG;
import org.qbicc.graph.CmpL;
import org.qbicc.graph.Convert;
import org.qbicc.graph.DataDeclarationHandle;
import org.qbicc.graph.DataHandle;
import org.qbicc.graph.DebugAddressDeclaration;
import org.qbicc.graph.Deref;
import org.qbicc.graph.Div;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.Extend;
import org.qbicc.graph.ExtractElement;
import org.qbicc.graph.ExtractMember;
import org.qbicc.graph.Fence;
import org.qbicc.graph.FunctionDeclarationHandle;
import org.qbicc.graph.FunctionHandle;
import org.qbicc.graph.GetAndAdd;
import org.qbicc.graph.GetAndBitwiseAnd;
import org.qbicc.graph.GetAndBitwiseOr;
import org.qbicc.graph.GetAndBitwiseXor;
import org.qbicc.graph.GetAndSet;
import org.qbicc.graph.GlobalVariable;
import org.qbicc.graph.Goto;
import org.qbicc.graph.If;
import org.qbicc.graph.InsertElement;
import org.qbicc.graph.InsertMember;
import org.qbicc.graph.Invoke;
import org.qbicc.graph.InvokeNoReturn;
import org.qbicc.graph.IsEq;
import org.qbicc.graph.IsGe;
import org.qbicc.graph.IsGt;
import org.qbicc.graph.IsLe;
import org.qbicc.graph.IsLt;
import org.qbicc.graph.IsNe;
import org.qbicc.graph.Load;
import org.qbicc.graph.MemberOf;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Mod;
import org.qbicc.graph.Multiply;
import org.qbicc.graph.Neg;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.NotNull;
import org.qbicc.graph.Or;
import org.qbicc.graph.ParameterValue;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.Return;
import org.qbicc.graph.Select;
import org.qbicc.graph.Shl;
import org.qbicc.graph.Shr;
import org.qbicc.graph.StackAllocation;
import org.qbicc.graph.Store;
import org.qbicc.graph.Sub;
import org.qbicc.graph.Switch;
import org.qbicc.graph.TailCall;
import org.qbicc.graph.TailInvoke;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Unreachable;
import org.qbicc.graph.Unschedulable;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.ValueReturn;
import org.qbicc.graph.Xor;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.machine.llvm.AsmFlag;
import org.qbicc.machine.llvm.FastMathFlag;
import org.qbicc.machine.llvm.FloatCondition;
import org.qbicc.machine.llvm.FunctionAttributes;
import org.qbicc.machine.llvm.FunctionDefinition;
import org.qbicc.machine.llvm.IntCondition;
import org.qbicc.machine.llvm.LLBasicBlock;
import org.qbicc.machine.llvm.LLBuilder;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.ParameterAttributes;
import org.qbicc.machine.llvm.Values;
import org.qbicc.machine.llvm.debuginfo.MetadataNode;
import org.qbicc.machine.llvm.impl.LLVM;
import org.qbicc.machine.llvm.op.AtomicRmw;
import org.qbicc.machine.llvm.op.Call;
import org.qbicc.machine.llvm.op.GetElementPtr;
import org.qbicc.machine.llvm.op.Instruction;
import org.qbicc.machine.llvm.op.OrderingConstraint;
import org.qbicc.machine.llvm.op.Phi;
import org.qbicc.machine.llvm.op.YieldingInstruction;
import org.qbicc.object.Function;
import org.qbicc.plugin.methodinfo.CallSiteInfo;
import org.qbicc.plugin.unwind.UnwindHelper;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.Type;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.MethodElement;

final class LLVMNodeVisitor implements NodeVisitor<Void, LLValue, Instruction, Instruction, Void> {
    final CompilationContext ctxt;
    final Module module;
    final LLVMModuleDebugInfo debugInfo;
    final LLVMPseudoIntrinsics pseudoIntrinsics;
    final LLValue topSubprogram;
    final LLVMModuleNodeVisitor moduleVisitor;
    final Schedule schedule;
    final Function functionObj;
    final FunctionDefinition func;
    final BasicBlock entryBlock;
    final Set<Action> visitedActions = new HashSet<>();
    final Map<Value, LLValue> mappedValues = new HashMap<>();
    final Map<BasicBlock, LLBasicBlock> mappedBlocks = new HashMap<>();
    final Map<BasicBlock, LLBasicBlock> mappedCatchBlocks = new HashMap<>();
    final MethodBody methodBody;
    final LLBuilder builder;
    final Map<Node, LLValue> inlineLocations = new HashMap<>();
    final Map<LocalVariableElement, MetadataNode> localVariables = new HashMap<>();

    private boolean personalityAdded;

    LLVMNodeVisitor(final CompilationContext ctxt, final Module module, final LLVMModuleDebugInfo debugInfo, final LLVMPseudoIntrinsics pseudoIntrinsics, final LLValue topSubprogram, final LLVMModuleNodeVisitor moduleVisitor, final Schedule schedule, final Function functionObj, final FunctionDefinition func) {
        this.ctxt = ctxt;
        this.module = module;
        this.debugInfo = debugInfo;
        this.pseudoIntrinsics = pseudoIntrinsics;
        this.topSubprogram = topSubprogram;
        this.moduleVisitor = moduleVisitor;
        this.schedule = schedule;
        this.functionObj = functionObj;
        this.func = func;
        this.methodBody = functionObj.getBody();
        entryBlock = methodBody.getEntryBlock();
        builder = LLBuilder.newBuilder(func.getRootBlock());
        personalityAdded = false;
    }

    // begin

    public void execute() {
        FunctionType funcType = functionObj.getType();
        int cnt = methodBody.getParameterCount();
        if (cnt != funcType.getParameterCount()) {
            throw new IllegalStateException("Mismatch between method body and function type parameter counts");
        }
        for (int i = 0; i < cnt; i ++) {
            ParameterValue value = functionObj.getBody().getParameterValue(i);
            ValueType      type = value.getType();
            org.qbicc.machine.llvm.Function.Parameter param = func.param(map(type)).name(value.getLabel() + value.getIndex());
            if (type instanceof IntegerType && ((IntegerType)type).getMinBits() < 32) {
                if (type instanceof SignedIntegerType) {
                    param.attribute(ParameterAttributes.signext);
                } else {
                    param.attribute(ParameterAttributes.zeroext);
                }
            } else if (type instanceof BooleanType) {
                param.attribute(ParameterAttributes.zeroext);
            }
            mappedValues.put(value, param.asValue());
        }
        ValueType retType = funcType.getReturnType();
        org.qbicc.machine.llvm.Function.Returns ret = func.returns(map(retType));
        if (retType instanceof IntegerType && ((IntegerType)retType).getMinBits() < 32) {
            if (retType instanceof SignedIntegerType) {
                ret.attribute(ParameterAttributes.signext);
            } else {
                ret.attribute(ParameterAttributes.zeroext);
            }
        } else if (retType instanceof BooleanType) {
            ret.attribute(ParameterAttributes.zeroext);
        }
        map(entryBlock);
    }

    // actions

    public Instruction visit(final Void param, final BlockEntry node) {
        // no operation
        return null;
    }

    private static final LLValue llvm_dbg_addr = Values.global("llvm.dbg.addr");
    private static final LLValue emptyExpr = diExpression().asValue();

    @Override
    public Instruction visit(final Void param, final DebugAddressDeclaration node) {
        map(node.getDependency());
        Value address = node.getAddress();
        LLValue mappedAddress = map(address);
        PointerType pointerType = (PointerType) address.getType();
        LLValue mappedPointerType = map(pointerType);
        ValueType actualType = pointerType.getPointeeType();
        LocalVariableElement variable = node.getVariable();
        MetadataNode metadataNode = localVariables.get(variable);
        if (metadataNode == null) {
            // first occurrence
            // todo: get alignment from variable
            metadataNode = module.diLocalVariable(variable.getName(), debugInfo.getType(actualType), topSubprogram, debugInfo.createSourceFile(node.getElement()), node.getSourceLine(), actualType.getAlign());
            localVariables.put(variable, metadataNode);
        }
        Call call = builder.call(void_, llvm_dbg_addr);
        call.arg(metadata(mappedPointerType), mappedAddress)
            .arg(metadata, metadataNode.asRef())
            .arg(metadata, emptyExpr);
        return call;
    }

    public Instruction visit(final Void param, final Store node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        org.qbicc.machine.llvm.op.Store storeInsn = builder.store(map(valueHandle.getPointerType()), map(node.getValue()), map(node.getValue().getType()), ptr);
        storeInsn.align(valueHandle.getValueType().getAlign());
        if (node.getMode() == MemoryAtomicityMode.SEQUENTIALLY_CONSISTENT) {
            storeInsn.atomic(OrderingConstraint.seq_cst);
        } else if (node.getMode() == MemoryAtomicityMode.UNORDERED) {
            storeInsn.atomic(OrderingConstraint.unordered);
        }
        return storeInsn;
    }

    public Instruction visit(final Void param, final Fence node) {
        map(node.getDependency());
        MemoryAtomicityMode mode = node.getAtomicityMode();
        switch (mode) {
            case ACQUIRE:
                return builder.fence(OrderingConstraint.acquire);
            case RELEASE:
                return builder.fence(OrderingConstraint.release);
            case ACQUIRE_RELEASE:
                return builder.fence(OrderingConstraint.acq_rel);
            case SEQUENTIALLY_CONSISTENT:
                return builder.fence(OrderingConstraint.seq_cst);
        }
        throw Assert.unreachableCode();
    }

    // terminators

    public Instruction visit(final Void param, final Goto node) {
        map(node.getDependency());
        return builder.br(map(node.getResumeTarget()));
    }

    public Instruction visit(final Void param, final If node) {
        map(node.getDependency());
        return builder.br(map(node.getCondition()), map(node.getTrueBranch()), map(node.getFalseBranch()));
    }

    public Instruction visit(final Void param, final Return node) {
        map(node.getDependency());
        return builder.ret();
    }

    public Instruction visit(final Void param, final Unreachable node) {
        map(node.getDependency());
        return builder.unreachable();
    }

    public Instruction visit(final Void param, final Switch node) {
        map(node.getDependency());
        org.qbicc.machine.llvm.op.Switch switchInst = builder.switch_(i32, map(node.getSwitchValue()), map(node.getDefaultTarget()));

        for (int i = 0; i < node.getNumberOfValues(); i++)
            switchInst.case_(Values.intConstant(node.getValueForIndex(i)), map(node.getTargetForIndex(i)));

        return switchInst;
    }

    public Instruction visit(final Void param, final ValueReturn node) {
        map(node.getDependency());
        return builder.ret(map(node.getReturnValue().getType()), map(node.getReturnValue()));
    }

    // values

    boolean isFloating(Type type) {
        return type instanceof FloatType;
    }

    boolean isSigned(Type type) {
        return type instanceof SignedIntegerType;
    }

    boolean isPointerLike(Type type) {
        return isPointer(type) || isReference(type);
    }

    boolean isPointer(Type type) {
        return type instanceof PointerType;
    }

    boolean isReference(Type type) {
        return type instanceof ReferenceType;
    }

    boolean isCollected(Type type) {
        if (type instanceof PointerType) {
            return ((PointerType) type).isCollected();
        } else if (type instanceof ReferenceType) {
            return true;
        } else {
            return false;
        }
    }

    public LLValue visit(final Void param, final Add node) {
        ValueType type = node.getType();
        LLValue inputType = map(type);
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(type) ?
               builder.fadd(inputType, llvmLeft, llvmRight).asLocal() :
               builder.add(inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final AddressOf node) {
        return node.getValueHandle().accept(GET_HANDLE_POINTER_VALUE, this);
    }

    public LLValue visit(final Void param, final And node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return builder.and(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    @Override
    public LLValue visit(Void param, Cmp node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());

        IntCondition lessThanCondition = isSigned(left.getType()) ? IntCondition.slt : IntCondition.ult;

        LLValue booleanType = map(ctxt.getTypeSystem().getBooleanType());
        LLValue integerType = map(ctxt.getTypeSystem().getSignedInteger32Type());
        return builder.select(
            booleanType,
            builder.icmp(lessThanCondition, inputType, llvmLeft, llvmRight).asLocal(),
            integerType,
            map(ctxt.getLiteralFactory().literalOf(-1)),
            builder.select(
                booleanType,
                builder.icmp(IntCondition.eq, inputType, llvmLeft, llvmRight).asLocal(),
                integerType,
                map(ctxt.getLiteralFactory().literalOf(0)),
                map(ctxt.getLiteralFactory().literalOf(1))
            ).asLocal()
        ).asLocal();
    }

    @Override
    public LLValue visit(Void param, CmpG node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());

        LLValue booleanType = map(ctxt.getTypeSystem().getBooleanType());
        LLValue integerType = map(ctxt.getTypeSystem().getSignedInteger32Type());
        return builder.select(
            booleanType,
            builder.fcmp(FloatCondition.ugt, inputType, llvmLeft, llvmRight).asLocal(),
            integerType,
            map(ctxt.getLiteralFactory().literalOf(1)),
            builder.select(
                booleanType,
                builder.fcmp(FloatCondition.ult, inputType, llvmLeft, llvmRight).withFlags(Set.of(FastMathFlag.nnan)).asLocal(),
                integerType,
                map(ctxt.getLiteralFactory().literalOf(-1)),
                map(ctxt.getLiteralFactory().literalOf(0))
            ).asLocal()
        ).asLocal();
    }

    @Override
    public LLValue visit(Void param, CmpL node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());

        LLValue booleanType = map(ctxt.getTypeSystem().getBooleanType());
        LLValue integerType = map(ctxt.getTypeSystem().getSignedInteger32Type());
        return builder.select(
            booleanType,
            builder.fcmp(FloatCondition.ult, inputType, llvmLeft, llvmRight).asLocal(),
            integerType,
            map(ctxt.getLiteralFactory().literalOf(-1)),
            builder.select(
                booleanType,
                builder.fcmp(FloatCondition.ugt, inputType, llvmLeft, llvmRight).withFlags(Set.of(FastMathFlag.nnan)).asLocal(),
                integerType,
                map(ctxt.getLiteralFactory().literalOf(1)),
                map(ctxt.getLiteralFactory().literalOf(0))
            ).asLocal()
        ).asLocal();
    }

    public LLValue visit(final Void param, final IsEq node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getLeftInput().getType()) ?
            builder.fcmp(FloatCondition.oeq, inputType, llvmLeft, llvmRight).asLocal() :
            builder.icmp(IntCondition.eq, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final IsNe node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getLeftInput().getType()) ?
            builder.fcmp(FloatCondition.one, inputType, llvmLeft, llvmRight).asLocal() :
            builder.icmp(IntCondition.ne, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final IsLt node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
            builder.fcmp(FloatCondition.olt, inputType, llvmLeft, llvmRight).asLocal() :
            isSigned(valueType) ?
                builder.icmp(IntCondition.slt, inputType, llvmLeft, llvmRight).asLocal() :
                builder.icmp(IntCondition.ult, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final IsLe node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
            builder.fcmp(FloatCondition.ole, inputType, llvmLeft, llvmRight).asLocal() :
            isSigned(valueType) ?
                builder.icmp(IntCondition.sle, inputType, llvmLeft, llvmRight).asLocal() :
                builder.icmp(IntCondition.ule, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final IsGt node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
            builder.fcmp(FloatCondition.ogt, inputType, llvmLeft, llvmRight).asLocal() :
            isSigned(valueType) ?
                builder.icmp(IntCondition.sgt, inputType, llvmLeft, llvmRight).asLocal() :
                builder.icmp(IntCondition.ugt, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final IsGe node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
            builder.fcmp(FloatCondition.oge, inputType, llvmLeft, llvmRight).asLocal() :
            isSigned(valueType) ?
                builder.icmp(IntCondition.sge, inputType, llvmLeft, llvmRight).asLocal() :
                builder.icmp(IntCondition.uge, inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Or node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return builder.or(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Xor node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return builder.xor(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Multiply node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getType()) ?
               builder.fmul(inputType, llvmLeft, llvmRight).asLocal() :
               builder.mul(inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Select node) {
        Value trueValue = node.getTrueValue();
        LLValue inputType = map(trueValue.getType());
        Value falseValue = node.getFalseValue();
        return builder.select(map(node.getCondition().getType()), map(node.getCondition()), inputType, map(trueValue), map(falseValue)).asLocal();
    }

    public LLValue visit(final Void param, final PhiValue node) {
        Phi phi = builder.phi(map(node.getType()));
        LLValue result = phi.asLocal();
        mappedValues.put(node, result);
        BasicBlock ourBlock = node.getPinnedBlock();
        for (BasicBlock incomingBlock : ourBlock.getIncoming()) {
            Terminator terminator = incomingBlock.getTerminator();
            Value v = node.getValueForInput(terminator);
            if (v != null) {
                // process dependencies
                LLBasicBlock incoming = map(incomingBlock);
                LLValue data = map(v);
                // we have to list the phi as many times as the predecessor enters this block
                int cnt = terminator.getSuccessorCount();
                for (int i = 0; i < cnt; i ++) {
                    if (terminator.getSuccessor(i) == ourBlock) {
                        phi.item(data, incoming);
                    }
                }
            }
        }
        return result;
    }

    public LLValue visit(final Void param, final Load node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        org.qbicc.machine.llvm.op.Load loadInsn = builder.load(map(valueHandle.getPointerType()), map(valueHandle.getValueType()), ptr);
        loadInsn.align(node.getType().getAlign());
        if (node.getMode() == MemoryAtomicityMode.ACQUIRE) {
            loadInsn.atomic(OrderingConstraint.acquire);
        } else if (node.getMode() == MemoryAtomicityMode.UNORDERED) {
            loadInsn.atomic(OrderingConstraint.unordered);
        }
        return loadInsn.asLocal();
    }

    public LLValue visit(Void param, GetAndAdd node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        AtomicRmw insn = builder.atomicrmw(map(valueHandle.getPointerType()), map(node.getUpdateValue()), map(node.getUpdateValue().getType()), ptr).add();
        insn.align(valueHandle.getValueType().getAlign());
        insn.ordering(getOC(node.getAtomicityMode()));
        return insn.asLocal();
    }

    public LLValue visit(Void param, GetAndBitwiseAnd node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        AtomicRmw insn = builder.atomicrmw(map(valueHandle.getPointerType()), map(node.getUpdateValue()), map(node.getUpdateValue().getType()), ptr).and();
        insn.align(valueHandle.getValueType().getAlign());
        insn.ordering(getOC(node.getAtomicityMode()));
        return insn.asLocal();
    }

    public LLValue visit(Void param, GetAndBitwiseOr node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        AtomicRmw insn = builder.atomicrmw(map(valueHandle.getPointerType()), map(node.getUpdateValue()), map(node.getUpdateValue().getType()), ptr).or();
        insn.align(valueHandle.getValueType().getAlign());
        insn.ordering(getOC(node.getAtomicityMode()));
        return insn.asLocal();
    }

    public LLValue visit(Void param, GetAndBitwiseXor node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        AtomicRmw insn = builder.atomicrmw(map(valueHandle.getPointerType()), map(node.getUpdateValue()), map(node.getUpdateValue().getType()), ptr).xor();
        insn.align(valueHandle.getValueType().getAlign());
        insn.ordering(getOC(node.getAtomicityMode()));
        return insn.asLocal();
    }

    public LLValue visit(Void param, GetAndSet node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        AtomicRmw insn = builder.atomicrmw(map(valueHandle.getPointerType()), map(node.getUpdateValue()), map(node.getUpdateValue().getType()), ptr).xchg();
        insn.align(valueHandle.getValueType().getAlign());
        insn.ordering(getOC(node.getAtomicityMode()));
        return insn.asLocal();
    }

    public LLValue visit(final Void param, final Neg node) {
        Type javaInputType = node.getInput().getType();
        LLValue inputType = map(javaInputType);
        LLValue llvmInput = map(node.getInput());
        return builder.fneg(inputType, llvmInput).asLocal();
    }

    public LLValue visit(Void param, NotNull node) {
        return map(node.getInput());
    }

    public LLValue visit(final Void param, final Shr node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isSigned(node.getType()) ?
               builder.ashr(inputType, llvmLeft, llvmRight).asLocal() :
               builder.lshr(inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Shl node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return builder.shl(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Sub node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getLeftInput().getType())
            ? builder.fsub(map(node.getType()), llvmLeft, llvmRight).asLocal()
            : builder.sub(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Div node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getType()) ?
               builder.fdiv(inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      builder.sdiv(inputType, llvmLeft, llvmRight).asLocal() :
                      builder.udiv(inputType, llvmLeft, llvmRight).asLocal();
    }

    public LLValue visit(final Void param, final Mod node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getType()) ?
               builder.frem(inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      builder.srem(inputType, llvmLeft, llvmRight).asLocal() :
                      builder.urem(inputType, llvmLeft, llvmRight).asLocal();
    }

    private LLValue createRefToPtrCast(final LLValue fromType, LLValue value, final LLValue toType) {
        if (!pseudoIntrinsics.getCollectedPtrType().equals(fromType)) {
            value = builder.bitcast(fromType, value, pseudoIntrinsics.getCollectedPtrType()).asLocal();
        }

        Call call = builder.call(pseudoIntrinsics.getCastRefToPtrType(), pseudoIntrinsics.getCastRefToPtr());
        call.arg(pseudoIntrinsics.getCollectedPtrType(), value);

        if (!pseudoIntrinsics.getRawPtrType().equals(toType)) {
            return builder.bitcast(pseudoIntrinsics.getRawPtrType(), call.asLocal(), toType).asLocal();
        } else {
            return call.asLocal();
        }
    }

    private LLValue createPtrToRefCast(final LLValue fromType, LLValue value, final LLValue toType) {
        if (!pseudoIntrinsics.getRawPtrType().equals(fromType)) {
            value = builder.bitcast(fromType, value, pseudoIntrinsics.getRawPtrType()).asLocal();
        }

        Call call = builder.call(pseudoIntrinsics.getCastPtrToRefType(), pseudoIntrinsics.getCastPtrToRef());
        call.arg(pseudoIntrinsics.getRawPtrType(), value);

        if (!pseudoIntrinsics.getCollectedPtrType().equals(toType)) {
            return builder.bitcast(pseudoIntrinsics.getCollectedPtrType(), call.asLocal(), toType).asLocal();
        } else {
            return call.asLocal();
        }
    }

    public LLValue visit(final Void param, final Deref node) {
        ctxt.error(node.getElement(), "Invalid dereference of %s", node.getInput().getType());
        return null;
    }

    public LLValue visit(final Void param, final BitCast node) {
        ValueType javaInputType = node.getInput().getType();
        ValueType javaOutputType = node.getType();
        LLValue llvmInput = map(node.getInput());
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        if (inputType.equals(outputType)) {
            return llvmInput;
        }

        if (isPointerLike(javaInputType) && isPointerLike(javaOutputType) && isCollected(javaInputType) != isCollected(javaOutputType)) {
            if (isCollected(javaInputType)) {
                return createRefToPtrCast(inputType, llvmInput, outputType);
            } else {
                return createPtrToRefCast(inputType, llvmInput, outputType);
            }
        } else {
            return builder.bitcast(inputType, llvmInput, outputType).asLocal();
        }
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

        if (isPointerLike(javaInputType)) {
            if (isPointerLike(javaOutputType)) {
                if (isCollected(javaInputType) != isCollected(javaOutputType)) {
                    if (isCollected(javaInputType)) {
                        return createRefToPtrCast(inputType, llvmInput, outputType);
                    } else {
                        return createPtrToRefCast(inputType, llvmInput, outputType);
                    }
                } else {
                    return builder.bitcast(inputType, llvmInput, outputType).asLocal();
                }
            } else if (javaOutputType instanceof IntegerType) {
                if (isCollected(javaInputType)) {
                    return builder.ptrtoint(
                        pseudoIntrinsics.getRawPtrType(),
                        createRefToPtrCast(inputType, llvmInput, pseudoIntrinsics.getRawPtrType()),
                        outputType
                    ).asLocal();
                } else {
                    return builder.ptrtoint(inputType, llvmInput, outputType).asLocal();
                }
            }
        } else if (javaInputType instanceof FloatType) {
            if (javaOutputType instanceof SignedIntegerType) {
                return builder.fptosi(inputType, llvmInput, outputType).asLocal();
            } else if (javaOutputType instanceof UnsignedIntegerType) {
                return builder.fptoui(inputType, llvmInput, outputType).asLocal();
            }
        } else if (javaInputType instanceof IntegerType) {
            if (isPointerLike(javaOutputType)) {
                if (isCollected(javaOutputType)) {
                    return createPtrToRefCast(
                        pseudoIntrinsics.getRawPtrType(),
                        builder.inttoptr(inputType, llvmInput, pseudoIntrinsics.getRawPtrType()).asLocal(),
                        outputType
                    );
                } else {
                    return builder.inttoptr(inputType, llvmInput, outputType).asLocal();
                }
            } else if (javaInputType instanceof SignedIntegerType) {
                if (javaOutputType instanceof FloatType) {
                    return builder.sitofp(inputType, llvmInput, outputType).asLocal();
                }
            } else if (javaInputType instanceof UnsignedIntegerType) {
                if (javaOutputType instanceof FloatType) {
                    return builder.uitofp(inputType, llvmInput, outputType).asLocal();
                }
            }
        }

        ctxt.error(functionObj.getOriginalElement(), node, "llvm: Unhandled conversion %s -> %s", javaInputType.toString(), javaOutputType.toString());
        return llvmInput;
    }

    public LLValue visit(final Void param, final Extend node) {
        WordType javaInputType = (WordType) node.getInput().getType();
        WordType javaOutputType = node.getType();
        LLValue llvmInput = map(node.getInput());
        if (javaInputType instanceof IntegerType && javaOutputType instanceof IntegerType && javaInputType.getMinBits() == javaOutputType.getMinBits()) {
            return llvmInput;
        }
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        return isFloating(javaInputType) ?
               builder.fpext(inputType, llvmInput, outputType).asLocal() :
                    isSigned(javaInputType) ?
                    builder.sext(inputType, llvmInput, outputType).asLocal() :
                    builder.zext(inputType, llvmInput, outputType).asLocal();
    }

    public LLValue visit(final Void param, final ExtractElement node) {
        LLValue arrayType = map(node.getArrayType());
        LLValue array = map(node.getArrayValue());
        LLValue index = map(node.getIndex());
        return builder.extractvalue(arrayType, array).arg(index).asLocal();
    }

    public LLValue visit(final Void param, final ExtractMember node) {
        LLValue compType = map(node.getCompoundType());
        LLValue comp = map(node.getCompoundValue());
        LLValue index = map(node.getCompoundType(), node.getMember());
        return builder.extractvalue(compType, comp).arg(index).asLocal();
    }

    public LLValue visit(final Void param, final InsertElement node) {
        LLValue arrayType = map(node.getType());
        LLValue array = map(node.getArrayValue());
        LLValue valueType = map(node.getInsertedValue().getType());
        LLValue value = map(node.getInsertedValue());
        LLValue index = map(node.getIndex());
        return builder.insertvalue(arrayType, array, valueType, value).arg(index).asLocal();
    }

    public LLValue visit(final Void param, final InsertMember node) {
        LLValue compType = map(node.getType());
        LLValue comp = map(node.getCompoundValue());
        LLValue valueType = map(node.getInsertedValue().getType());
        LLValue value = map(node.getInsertedValue());
        LLValue index = map(node.getType(), node.getMember());
        return builder.insertvalue(compType, comp, valueType, value).arg(index).asLocal();
    }

    public LLValue visit(final Void param, final Invoke.ReturnValue node) {
        map(node.getDependency());
        LLBasicBlock invokeBlock = map(node.getInvoke().getTerminatedBlock());
        // should already be registered by now in most cases
        LLValue llValue = mappedValues.get(node);
        if (llValue == null) {
            // map late
            postMap(node.getInvoke().getTerminatedBlock(), invokeBlock);
            llValue = mappedValues.get(node);
        }
        return llValue;
    }

    public LLValue visit(final Void param, final CheckCast node) {
        map(node.getDependency());
        return map(node.getInput());
    }

    public LLValue visit(final Void param, final Truncate node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        LLValue llvmInput = map(node.getInput());
        return isFloating(javaInputType) ?
               builder.ftrunc(inputType, llvmInput, outputType).asLocal() :
               builder.trunc(inputType, llvmInput, outputType).asLocal();
    }

    public LLValue visit(final Void param, final StackAllocation node) {
        map(node.getDependency());
        LLValue pointeeType = map(node.getType().getPointeeType());
        LLValue countType = map(node.getCount().getType());
        LLValue count = map(node.getCount());
        LLValue alignment = map(node.getAlign());
        return builder.alloca(pointeeType).elements(countType, count).align(alignment).asLocal();
    }

    // calls

    public LLValue visit(Void param, org.qbicc.graph.Call node) {
        map(node.getDependency());
        FunctionType functionType = node.getFunctionType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        ValueHandle valueHandle = node.getValueHandle();
        LLValue llTarget = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        Call call = builder.call(llType, llTarget).noTail();
        setCallArguments(call, arguments);
        setCallReturnValue(call, functionType);
        addStatepointId(call, node);
        return call.asLocal();
    }

    @Override
    public LLValue visit(Void param, CallNoSideEffects node) {
        FunctionType functionType = node.getFunctionType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        ValueHandle valueHandle = node.getValueHandle();
        LLValue llTarget;
        llTarget = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        Call call = builder.call(llType, llTarget).noTail();
        setCallArguments(call, arguments);
        setCallReturnValue(call, functionType);
        addStatepointId(call, node);
        return call.asLocal();
    }

    @Override
    public Instruction visit(Void param, CallNoReturn node) {
        map(node.getDependency());
        FunctionType functionType = node.getFunctionType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        ValueHandle valueHandle = node.getValueHandle();
        LLValue llTarget;
        llTarget = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        Call call = builder.call(llType, llTarget).noTail().attribute(FunctionAttributes.noreturn);
        setCallArguments(call, arguments);
        setCallReturnValue(call, functionType);
        addStatepointId(call, node);
        builder.unreachable();
        return call;
    }

    @Override
    public Instruction visit(Void param, TailCall node) {
        map(node.getDependency());
        FunctionType functionType = node.getFunctionType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        ValueHandle valueHandle = node.getValueHandle();
        LLValue llTarget;
        llTarget = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        Call call = builder.call(llType, llTarget).tail(); // hint only
        setCallArguments(call, arguments);
        setCallReturnValue(call, functionType);
        addStatepointId(call, node);
        ValueType returnType = node.getFunctionType().getReturnType();
        if (returnType instanceof VoidType) {
            return builder.ret();
        } else {
            return builder.ret(map(returnType), call.asLocal());
        }
    }

    @Override
    public Instruction visit(Void param, Invoke node) {
        map(node.getDependency());
        FunctionType functionType = node.getFunctionType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        ValueHandle valueHandle = node.getValueHandle();
        LLValue llTarget = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        if (mappedValues.containsKey(node.getReturnValue())) {
            // already done
            return null;
        }
        LLBasicBlock resume = checkMap(node.getResumeTarget());
        boolean postMapResume = resume == null;
        if (postMapResume) {
            resume = preMap(node.getResumeTarget());
        }
        LLBasicBlock catch_ = checkMap(node.getCatchBlock());
        boolean postMapCatch = catch_ == null;
        if (postMapCatch) {
            catch_ = preMap(node.getCatchBlock());
        }
        Call call = builder.invoke(llType, llTarget, resume, mapCatch(node.getCatchBlock()));
        mappedValues.put(node.getReturnValue(), call.asLocal());
        if (postMapResume) {
            postMap(node.getResumeTarget(), resume);
        }
        if (postMapCatch) {
            postMap(node.getCatchBlock(), catch_);
        }
        setCallArguments(call, arguments);
        setCallReturnValue(call, functionType);
        addPersonalityIfNeeded();
        addStatepointId(call, node);
        return call;
    }

    @Override
    public Instruction visit(Void param, InvokeNoReturn node) {
        map(node.getDependency());
        FunctionType functionType = node.getFunctionType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        ValueHandle valueHandle = node.getValueHandle();
        LLValue llTarget = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        LLBasicBlock unreachableTarget = func.createBlock();
        LLVM.newBuilder(unreachableTarget).unreachable();
        LLBasicBlock catch_ = checkMap(node.getCatchBlock());
        boolean postMapCatch = catch_ == null;
        if (postMapCatch) {
            catch_ = preMap(node.getCatchBlock());
        }
        Call call = builder.invoke(llType, llTarget, unreachableTarget, mapCatch(node.getCatchBlock())).attribute(FunctionAttributes.noreturn);
        if (postMapCatch) {
            postMap(node.getCatchBlock(), catch_);
        }
        setCallArguments(call, arguments);
        setCallReturnValue(call, functionType);
        addPersonalityIfNeeded();
        addStatepointId(call, node);
        return call;
    }

    @Override
    public Instruction visit(Void param, TailInvoke node) {
        map(node.getDependency());
        FunctionType functionType = node.getFunctionType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        ValueHandle valueHandle = node.getValueHandle();
        LLValue llTarget = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        LLBasicBlock catch_ = checkMap(node.getCatchBlock());
        boolean postMapCatch = catch_ == null;
        if (postMapCatch) {
            catch_ = preMap(node.getCatchBlock());
        }
        LLBasicBlock tailTarget = func.createBlock();
        Call call = builder.invoke(llType, llTarget, tailTarget, mapCatch(node.getCatchBlock()));
        if (postMapCatch) {
            postMap(node.getCatchBlock(), catch_);
        }
        setCallArguments(call, arguments);
        setCallReturnValue(call, functionType);
        ValueType returnType = node.getFunctionType().getReturnType();
        if (returnType instanceof VoidType) {
            LLVM.newBuilder(tailTarget).ret();
        } else {
            LLVM.newBuilder(tailTarget).ret(map(returnType), call.asLocal());
        }
        addPersonalityIfNeeded();
        addStatepointId(call, node);
        return call;
    }

    private void preMapArgumentList(final List<Value> arguments) {
        for (Value argument : arguments) {
            map(argument.getType());
            map(argument);
        }
    }

    private void setCallReturnValue(final Call call, final FunctionType functionType) {
        ValueType retType = functionType.getReturnType();
        Call.Returns ret = call.returns();
        if (retType instanceof IntegerType && ((IntegerType) retType).getMinBits() < 32) {
            if (retType instanceof SignedIntegerType) {
                ret.attribute(ParameterAttributes.signext);
            } else {
                ret.attribute(ParameterAttributes.zeroext);
            }
        } else if (retType instanceof BooleanType) {
            ret.attribute(ParameterAttributes.zeroext);
        }
    }

    private void setCallArguments(final Call call, final List<Value> arguments) {
        for (Value argument : arguments) {
            ValueType type = argument.getType();
            Call.Argument arg = call.arg(map(type), map(argument));
            if (type instanceof IntegerType && ((IntegerType) type).getMinBits() < 32) {
                if (type instanceof SignedIntegerType) {
                    arg.attribute(ParameterAttributes.signext);
                } else {
                    arg.attribute(ParameterAttributes.zeroext);
                }
            } else if (type instanceof BooleanType) {
                arg.attribute(ParameterAttributes.zeroext);
            }
        }
    }

    private void addPersonalityIfNeeded() {
        if (!personalityAdded) {
            MethodElement personalityFunction = UnwindHelper.get(ctxt).getPersonalityMethod();
            SymbolLiteral literal = ctxt.getLiteralFactory().literalOfSymbol(personalityFunction.getName(), personalityFunction.getType().getPointer());
            // clang generates the personality argument like this (by casting the function to i8* using bitcast):
            //      define dso_local void @_Z7catchitv() #0 personality i8* bitcast (i32 (...)* @__gxx_personality_v0 to i8*) {
            // We can also generate it this way using following construct:
            //      func.personality(Values.bitcastConstant(map(literal), map(literal.getType()), ptrTo(i8)), ptrTo(i8));
            // but directly specifying the function works as well.
            func.personality(map(literal), map(literal.getType()));
            personalityAdded = true;
        }
    }

    private void addStatepointId(Call call, Node node) {
        int statepointId = LLVM.getNextStatepointId();
        call.attribute(FunctionAttributes.statepointId(statepointId));
        CallSiteInfo.get(ctxt).mapStatepointIdToNode(statepointId, node);
    }

    // GEP

    private static final ValueHandleVisitor<LLVMNodeVisitor, GetElementPtr> GET_HANDLE_ELEMENT_POINTER = new ValueHandleVisitor<LLVMNodeVisitor, GetElementPtr>() {
        @Override
        public GetElementPtr visitUnknown(LLVMNodeVisitor param, ValueHandle node) {
            throw new IllegalStateException("Unexpected handle " + node);
        }

        @Override
        public GetElementPtr visit(LLVMNodeVisitor param, DataDeclarationHandle node) {
            return param.gep(Values.global(node.getProgramObject().getName()), node).arg(false, i32, ZERO);
        }

        @Override
        public GetElementPtr visit(LLVMNodeVisitor param, DataHandle node) {
            return param.gep(Values.global(node.getProgramObject().getName()), node).arg(false, i32, ZERO);
        }

        @Override
        public GetElementPtr visit(LLVMNodeVisitor param, ElementOf node) {
            ValueHandle nextHandle = node.getValueHandle();
            LLValue index = param.map(node.getIndex());
            LLValue indexType = param.map(node.getIndex().getType());
            if (nextHandle instanceof PointerHandle) {
                PointerHandle ptrHandle = (PointerHandle) nextHandle;
                // special case: element-of-pointer
                GetElementPtr gep = param.gep(param.map(ptrHandle.getPointerValue()), ptrHandle);
                gep.comment("index [" + node.getIndex() + "]");
                return gep.arg(false, indexType, index);
            }
            return nextHandle.accept(this, param).arg(false, indexType, index);
        }

        @Override
        public GetElementPtr visit(LLVMNodeVisitor param, GlobalVariable node) {
            GlobalVariableElement gv = node.getVariableElement();
            return param.gep(Values.global(gv.getName()), node).arg(false, i32, ZERO);
        }

        @Override
        public GetElementPtr visit(LLVMNodeVisitor param, MemberOf node) {
            LLValue index = param.map(node.getStructType(), node.getMember());
            GetElementPtr gep = node.getValueHandle().accept(this, param);
            gep.comment("member " + node.getMember().getName());
            return gep.arg(false, i32, index);
        }

        @Override
        public GetElementPtr visit(LLVMNodeVisitor param, PointerHandle node) {
            return param.gep(param.map(node.getPointerValue()), node).arg(false, i32, ZERO);
        }

        @Override
        public GetElementPtr visit(LLVMNodeVisitor param, ReferenceHandle node) {
            return param.gep(param.map(node.getReferenceValue()), node).arg(false, i32, ZERO);
        }
    };

    private static final ValueHandleVisitor<LLVMNodeVisitor, LLValue> GET_HANDLE_POINTER_VALUE = new ValueHandleVisitor<LLVMNodeVisitor, LLValue>() {
        @Override
        public LLValue visitUnknown(LLVMNodeVisitor param, ValueHandle node) {
            throw new IllegalStateException("Unexpected handle " + node);
        }

        @Override
        public LLValue visit(LLVMNodeVisitor param, AsmHandle node) {
            // special case: not a pointer at all!
            return Values.asm(node.getInstruction(), node.getConstraints(), map(node.getFlags()));
        }

        @Override
        public LLValue visit(LLVMNodeVisitor param, DataDeclarationHandle node) {
            return Values.global(node.getProgramObject().getName());
        }

        @Override
        public LLValue visit(LLVMNodeVisitor param, DataHandle node) {
            return Values.global(node.getProgramObject().getName());
        }

        @Override
        public LLValue visit(LLVMNodeVisitor param, ElementOf node) {
            return node.accept(GET_HANDLE_ELEMENT_POINTER, param).asLocal();
        }

        @Override
        public LLValue visit(LLVMNodeVisitor param, FunctionDeclarationHandle node) {
            return Values.global(node.getProgramObject().getName());
        }

        @Override
        public LLValue visit(LLVMNodeVisitor param, FunctionHandle node) {
            return Values.global(node.getProgramObject().getName());
        }

        @Override
        public LLValue visit(LLVMNodeVisitor param, GlobalVariable node) {
            return Values.global(node.getVariableElement().getName());
        }

        @Override
        public LLValue visit(LLVMNodeVisitor param, MemberOf node) {
            return node.accept(GET_HANDLE_ELEMENT_POINTER, param).asLocal();
        }

        @Override
        public LLValue visit(LLVMNodeVisitor param, PointerHandle node) {
            return param.map(node.getPointerValue());
        }
    };

    private static Set<AsmFlag> map(final Set<AsmHandle.Flag> flags) {
        EnumSet<AsmFlag> output = EnumSet.noneOf(AsmFlag.class);
        if (flags.contains(AsmHandle.Flag.SIDE_EFFECT)) {
            output.add(AsmFlag.SIDE_EFFECT);
        }
        if (! flags.contains(AsmHandle.Flag.NO_THROW)) {
            output.add(AsmFlag.UNWIND);
        }
        if (flags.contains(AsmHandle.Flag.ALIGN_STACK)) {
            output.add(AsmFlag.ALIGN_STACK);
        }
        if (flags.contains(AsmHandle.Flag.INTEL_DIALECT)) {
            output.add(AsmFlag.INTEL_DIALECT);
        }
        return output;
    }

    GetElementPtr gep(LLValue ptr, ValueHandle handle) {
        PointerType pointerType = handle.getPointerType();
        return builder.getelementptr(map(pointerType.getPointeeType()), map(pointerType), ptr);
    }

    private OrderingConstraint getOC(MemoryAtomicityMode mode) {
        switch (mode) {
            case MONOTONIC:
                return OrderingConstraint.monotonic;
            case ACQUIRE:
                return OrderingConstraint.acquire;
            case RELEASE:
                return OrderingConstraint.release;
            case ACQUIRE_RELEASE:
                return OrderingConstraint.acq_rel;
            case SEQUENTIALLY_CONSISTENT:
                return OrderingConstraint.seq_cst;
        }
        throw Assert.unreachableCode();
    }

    @Override
    public LLValue visit(final Void param, final CmpAndSwap node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptrType = map(valueHandle.getPointerType());
        LLValue type = map(valueHandle.getValueType());
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        LLValue expect = map(node.getExpectedValue());
        LLValue update = map(node.getUpdateValue());
        OrderingConstraint successOrdering = getOC(node.getSuccessAtomicityMode());
        OrderingConstraint failureOrdering = getOC(node.getFailureAtomicityMode());
        org.qbicc.machine.llvm.op.CmpAndSwap cmpAndSwapBuilder = builder.cmpAndSwap(
            ptrType, type, ptr, expect, update, successOrdering, failureOrdering);
        if (node.getStrength() == CmpAndSwap.Strength.WEAK) {
            cmpAndSwapBuilder.weak();
        }
        return cmpAndSwapBuilder.asLocal();
    }

    // unknown node catch-all methods

    public LLValue visitUnknown(final Void param, final Value node) {
        return node.accept(moduleVisitor, null);
    }

    public Instruction visitUnknown(final Void param, final Action node) {
        ctxt.error(functionObj.getOriginalElement(), node, "llvm: Unrecognized action %s", node.getClass());
        return null;
    }

    public Instruction visitUnknown(final Void param, final Terminator node) {
        ctxt.error(functionObj.getOriginalElement(), node, "llvm: Unrecognized terminator %s", node.getClass());
        return null;
    }

    // mapping

    private LLValue createDbgLocation(final Node node, final boolean distinct) {
        LLValue inlinedAt = dbgInlinedCallSite(node.getCallSite());

        if (inlinedAt == null && node.getElement() != functionObj.getOriginalElement()) {
            ctxt.error(Location.builder().setNode(node).build(), "LLVM: Node is not part of the root function, but has no call site");
        }

        LLValue scope = (topSubprogram != null && inlinedAt == null)
                ? topSubprogram
                : debugInfo.getDebugInfoForFunction(node.getElement()).getScope(node.getBytecodeIndex());

        if (distinct) {
            return module.diLocation(node.getSourceLine(), 0, scope, inlinedAt).distinct(true).asRef();
        } else {
            return debugInfo.createDeduplicatedLocation(node.getSourceLine(), 0, scope, inlinedAt);
        }
    }

    private LLValue dbgInlinedCallSite(final Node node) {
        if (node == null) {
            return null;
        }

        LLValue diLocation = inlineLocations.get(node);

        if (diLocation == null) {
            diLocation = createDbgLocation(node, true);
            inlineLocations.put(node, diLocation);
        }

        return diLocation;
    }

    private LLValue dbg(final Node node) {
        if (node.getElement() == null || debugInfo == null) {
            return null;
        }

        return createDbgLocation(node, false);
    }

    private LLBasicBlock map(BasicBlock block) {
        LLBasicBlock mapped = checkMap(block);
        if (mapped != null) {
            return mapped;
        }
        return postMap(block, preMap(block));
    }

    private LLBasicBlock checkMap(final BasicBlock block) {
        return mappedBlocks.get(block);
    }

    private LLBasicBlock preMap(final BasicBlock block) {
        LLBasicBlock mapped = func.createBlock();
        mappedBlocks.put(block, mapped);
        return mapped;
    }

    private LLBasicBlock postMap(final BasicBlock block, LLBasicBlock mapped) {
        Terminator terminator = block.getTerminator();
        LLValue oldBuilderDebugLocation = builder.setDebugLocation(dbg(terminator));
        LLBasicBlock oldBuilderBlock = builder.moveToBlock(mapped);

        Instruction instruction = terminator.accept(this, null);
        addLineComment(terminator, instruction);

        builder.setDebugLocation(oldBuilderDebugLocation);
        builder.moveToBlock(oldBuilderBlock);

        return mapped;
    }

    private LLBasicBlock mapCatch(BasicBlock block) {
        LLBasicBlock mapped = mappedCatchBlocks.get(block);
        if (mapped != null) {
            return mapped;
        }

        mapped = func.createBlock();

        // TODO Is it correct to use the call's debug info here?
        LLBasicBlock oldBuilderBlock = builder.moveToBlock(mapped);

        builder.landingpad(token).cleanup();
        LLBasicBlock handler = map(block);
        builder.br(handler);

        builder.moveToBlock(oldBuilderBlock);
        return mapped;
    }

    private LLValue map(Type type) {
        return moduleVisitor.map(type);
    }

    private void map(Action action) {
        if (visitedActions.add(action)) {
            LLValue oldBuilderDebugLocation = builder.setDebugLocation(dbg(action));
            LLBasicBlock oldBuilderBlock = builder.moveToBlock(map(schedule.getBlockForNode(action)));

            Instruction instruction = action.accept(this, null);
            addLineComment(action, instruction);

            builder.setDebugLocation(oldBuilderDebugLocation);
            builder.moveToBlock(oldBuilderBlock);
        }
    }

    private LLValue map(Value value) {
        LLValue mapped = mappedValues.get(value);
        if (mapped != null) {
            return mapped;
        }

        if (value instanceof Unschedulable) {
            return value.accept(this, null);
        }

        LLBasicBlock oldBuilderBlock = builder.moveToBlock(map(schedule.getBlockForNode(value)));

        mapped = mappedValues.get(value);
        if (mapped == null) {
            LLValue oldBuilderDebugLocation = builder.setDebugLocation(dbg(value));
            mapped = value.accept(this, null);
            YieldingInstruction instruction = mapped.getInstruction();
            addLineComment(value, instruction);
            mappedValues.put(value, mapped);
            builder.setDebugLocation(oldBuilderDebugLocation);
        }

        builder.moveToBlock(oldBuilderBlock);
        return mapped;
    }

    private void addLineComment(final Node node, final Instruction instruction) {
        if (instruction != null) {
            instruction.comment(node.getElement().getSourceFileName() + ":" + node.getSourceLine() + " bci@" + node.getBytecodeIndex());
        }
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
        return moduleVisitor.map(compoundType, member);
    }
}
