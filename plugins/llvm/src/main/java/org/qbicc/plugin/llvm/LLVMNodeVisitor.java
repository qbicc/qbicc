package org.qbicc.plugin.llvm;

import static org.qbicc.graph.atomic.AccessModes.*;
import static org.qbicc.machine.llvm.Types.*;
import static org.qbicc.machine.llvm.Values.*;

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
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.CallNoReturn;
import org.qbicc.graph.CallNoSideEffects;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.Cmp;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.CmpG;
import org.qbicc.graph.CmpL;
import org.qbicc.graph.Comp;
import org.qbicc.graph.Convert;
import org.qbicc.graph.DebugAddressDeclaration;
import org.qbicc.graph.DebugValueDeclaration;
import org.qbicc.graph.Div;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.Extend;
import org.qbicc.graph.ExtractElement;
import org.qbicc.graph.ExtractMember;
import org.qbicc.graph.Fence;
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
import org.qbicc.graph.Mod;
import org.qbicc.graph.Multiply;
import org.qbicc.graph.Neg;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.NotNull;
import org.qbicc.graph.Or;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.Reachable;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.Ret;
import org.qbicc.graph.Return;
import org.qbicc.graph.Select;
import org.qbicc.graph.Shl;
import org.qbicc.graph.Shr;
import org.qbicc.graph.Slot;
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
import org.qbicc.graph.VaArg;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.Xor;
import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.PointerLiteral;
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
import org.qbicc.machine.llvm.LazyLLValue;
import org.qbicc.machine.llvm.Module;
import org.qbicc.machine.llvm.ParameterAttributes;
import org.qbicc.machine.llvm.Values;
import org.qbicc.machine.llvm.debuginfo.DILocalVariable;
import org.qbicc.machine.llvm.debuginfo.DIOpcode;
import org.qbicc.machine.llvm.debuginfo.MetadataNode;
import org.qbicc.machine.llvm.impl.LLVM;
import org.qbicc.machine.llvm.op.AtomicRmw;
import org.qbicc.machine.llvm.op.Call;
import org.qbicc.machine.llvm.op.GetElementPtr;
import org.qbicc.machine.llvm.op.IndirectBranch;
import org.qbicc.machine.llvm.op.Instruction;
import org.qbicc.machine.llvm.op.OrderingConstraint;
import org.qbicc.machine.llvm.op.Phi;
import org.qbicc.machine.llvm.op.YieldingInstruction;
import org.qbicc.object.Function;
import org.qbicc.plugin.methodinfo.CallSiteInfo;
import org.qbicc.plugin.unwind.UnwindExceptionStrategy;
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
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.ParameterElement;

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
    final Map<Invoke, LLValue> invokeResults = new HashMap<>();
    final Map<BasicBlock, Map<Slot, Phi>> mappedBlockParamPhis = new HashMap<>();
    final Map<BasicBlock, Map<Slot, LLValue>> mappedBlockParams = new HashMap<>();
    final Map<BasicBlock, LLBasicBlock> mappedBlocks = new HashMap<>();
    final Map<BasicBlock, LLBasicBlock> mappedCatchBlocks = new HashMap<>();
    final MethodBody methodBody;
    final LLBuilder builder;
    final Map<Node, LLValue> inlineLocations = new HashMap<>();
    final Map<LocalVariableElement, DILocalVariable> localVariables = new HashMap<>();

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
        FunctionType funcType = functionObj.getValueType();
        int cnt = methodBody.getParameterCount();
        if (cnt != funcType.getParameterCount()) {
            throw new IllegalStateException("Mismatch between method body and function type parameter counts");
        }
        MethodBody methodBody = functionObj.getBody();
        for (int i = 0; i < cnt; i ++) {
            Slot slot = methodBody.getParameterSlot(i);
            ValueType type = funcType.getParameterType(i);
            org.qbicc.machine.llvm.Function.Parameter param = func.param(map(type)).name(slot.toString());
            if (type instanceof IntegerType && ((IntegerType)type).getMinBits() < 32) {
                if (type instanceof SignedIntegerType) {
                    param.attribute(ParameterAttributes.signext);
                } else {
                    param.attribute(ParameterAttributes.zeroext);
                }
            } else if (type instanceof BooleanType) {
                param.attribute(ParameterAttributes.zeroext);
            }
            mappedBlockParams.computeIfAbsent(entryBlock, LLVMNodeVisitor::newMap).put(slot, param.asValue());
        }
        ValueType retType = funcType.getReturnType();
        org.qbicc.machine.llvm.Function.Returns ret = func.returns(map(retType));
        if (retType instanceof IntegerType it && it.getMinBits() < 32) {
            if (retType instanceof SignedIntegerType) {
                ret.attribute(ParameterAttributes.signext);
            } else {
                ret.attribute(ParameterAttributes.zeroext);
            }
        } else if (retType instanceof BooleanType) {
            ret.attribute(ParameterAttributes.zeroext);
        }
        map(entryBlock);
        // fill in phi values
        for (Map.Entry<BasicBlock, Map<Slot, Phi>> entry : mappedBlockParamPhis.entrySet()) {
            BasicBlock block = entry.getKey();
            LLBasicBlock mappedBlock = checkMap(block);
            if (mappedBlock != null) {
                Map<Slot, Phi> valuesBySlot = entry.getValue();
                for (Map.Entry<Slot, Phi> subEntry : valuesBySlot.entrySet()) {
                    Slot slot = subEntry.getKey();
                    Phi phi = subEntry.getValue();
                    LLBasicBlock oldBlock = builder.moveToBlock(mappedBlock);
                    try {
                        // populate possible incoming values
                        for (BasicBlock incoming : block.getIncoming()) {
                            Terminator t = incoming.getTerminator();
                            cnt = t.getSuccessorCount();
                            LLBasicBlock mappedIncoming = checkMap(incoming);
                            // make sure there is one item per path from the predecessor
                            for (int i = 0; i < cnt; i ++) {
                                if (t.getSuccessor(i) == block) {
                                    if (slot == Slot.result() && t instanceof Invoke inv) {
                                        phi.item(invokeResults.get(inv), mappedIncoming);
                                    } else {
                                        phi.item(map(t.getOutboundArgument(slot)), mappedIncoming);
                                    }
                                }
                            }
                        }
                    } finally {
                        builder.moveToBlock(oldBlock);
                    }
                }
            }
        }
    }

    // actions

    public Instruction visit(final Void param, final BlockEntry node) {
        // no operation
        return null;
    }

    private static final LLValue llvm_dbg_value = Values.global("llvm.dbg.value");
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
        MetadataNode metadataNode = getLocalVariableMetadataNode(node, actualType, variable);
        Call call = builder.call(void_, llvm_dbg_value);
        call.arg(metadata(mappedPointerType), mappedAddress)
            .arg(metadata, metadataNode.asRef())
            .arg(metadata, LLVM.diExpression().arg(DIOpcode.Deref).asValue());
        return call;
    }

    @Override
    public Instruction visit(final Void param, final DebugValueDeclaration node) {
        map(node.getDependency());
        Value value = node.getValue();
        LLValue mappedValue = map(value);
        ValueType valueType = value.getType();
        LLValue mappedValueType = map(valueType);
        LocalVariableElement variable = node.getVariable();
        MetadataNode metadataNode = getLocalVariableMetadataNode(node, valueType, variable);
        Call call = builder.call(void_, llvm_dbg_value);
        call.arg(metadata(mappedValueType), mappedValue)
            .arg(metadata, metadataNode.asRef())
            .arg(metadata, emptyExpr);
        return call;
    }

    private MetadataNode getLocalVariableMetadataNode(final Node node, final ValueType valueType, final LocalVariableElement variable) {
        DILocalVariable metadataNode = localVariables.get(variable);
        if (metadataNode == null) {
            // first occurrence
            // todo: get alignment from variable
            metadataNode = module.diLocalVariable(variable.getName(), debugInfo.getType(variable.getType()), topSubprogram, debugInfo.createSourceFile(node.getElement()), node.getSourceLine(), valueType.getAlign());
            ParameterElement param = variable.getReflectsParameter();
            if (param != null) {
                // debug args are 1-based
                int index = ((InvokableElement) functionObj.getOriginalElement()).getParameters().indexOf(param);
                metadataNode.argument(index + 1);
            }
            localVariables.put(variable, metadataNode);
        }
        return metadataNode;
    }

    public Instruction visit(final Void param, final Store node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        org.qbicc.machine.llvm.op.Store storeInsn = builder.store(map(valueHandle.getType()), map(node.getValue()), map(node.getValue().getType()), ptr);
        storeInsn.align(valueHandle.getPointeeType().getAlign());
        WriteAccessMode accessMode = node.getAccessMode();
        if (SingleUnshared.includes(accessMode)) {
            // do nothing; not atomic
        } else if (SinglePlain.includes(accessMode)) {
            storeInsn.atomic(OrderingConstraint.unordered);
        } else if (SingleOpaque.includes(accessMode)) {
            storeInsn.atomic(OrderingConstraint.monotonic);
        } else if (SingleRelease.includes(accessMode)) {
            storeInsn.atomic(OrderingConstraint.release);
        } else if (SingleSeqCst.includes(accessMode)) {
            storeInsn.atomic(OrderingConstraint.seq_cst);
        } else {
            throw new IllegalArgumentException("LLVM store does not directly support access mode " + accessMode);
        }
        return storeInsn;
    }

    public Instruction visit(final Void param, final Fence node) {
        map(node.getDependency());
        GlobalAccessMode gam = node.getAccessMode();
        // no-op fences are removed already
        if (GlobalAcquire.includes(gam)) {
            return builder.fence(OrderingConstraint.acquire);
        } else if (GlobalRelease.includes(gam)) {
            return builder.fence(OrderingConstraint.release);
        } else if (GlobalAcqRel.includes(gam)) {
            return builder.fence(OrderingConstraint.acq_rel);
        } else {
            return builder.fence(OrderingConstraint.seq_cst);
        }
    }

    public Instruction visit(final Void unused, final Reachable node) {
        map(node.getDependency());
        map(node.getReachableValue());
        // nothing actually emitted
        return null;
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

    public Instruction visit(final Void param, final Ret node) {
        map(node.getDependency());
        List<BasicBlock> successors = node.getSuccessors();
        if (successors.size() == 1) {
            return builder.br(map(successors.get(0)));
        }
        IndirectBranch ibr = builder.indirectbr(map(node.getReturnAddressValue()));
        for (BasicBlock successor : successors) {
            ibr.possibleTarget(map(successor));
        }
        return ibr;
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

    public Instruction visit(final Void param, final Return node) {
        map(node.getDependency());
        final ValueType retType = node.getReturnValue().getType();
        if (retType instanceof VoidType) {
            return builder.ret();
        } else {
            return builder.ret(map(retType), map(node.getReturnValue()));
        }
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
    public LLValue visit(Void unused, BlockParameter node) {
        BasicBlock block = node.getPinnedBlock();
        Map<Slot, LLValue> subMap = mappedBlockParams.computeIfAbsent(block, LLVMNodeVisitor::newMap);
        Slot slot = node.getSlot();
        LLValue llValue = subMap.get(slot);
        if (llValue == null) {
            LLBasicBlock oldBlock = builder.moveToBlock(map(block));
            Phi phi = builder.phi(map(node.getType()));
            mappedBlockParamPhis.computeIfAbsent(block, LLVMNodeVisitor::newMap).put(slot, phi);
            builder.moveToBlock(oldBlock);
            llValue = phi.asLocal(node.appendQualifiedName(new StringBuilder()).toString());
            // pre-seed
            mappedValues.put(node, llValue);
            subMap.put(slot, llValue);
            for (BasicBlock incoming : block.getIncoming()) {
                Terminator t = incoming.getTerminator();
                int cnt = t.getSuccessorCount();
                for (int i = 0; i < cnt; i ++) {
                    if (t.getSuccessor(i) == block && ! t.isImplicitOutboundArgument(slot, block)) {
                        // make sure it's mapped for post-processing
                        map(t.getOutboundArgument(slot));
                    }
                }
            }
        }
        return llValue;
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

    public LLValue visit(final Void param, final Comp node) {
        final Value input = node.getInput();
        final LLValue inputType = map(input.getType());
        final LLValue llvmInput = map(input);
        if (input.getType() instanceof BooleanType) {
            return builder.xor(inputType, llvmInput, TRUE).asLocal();
        } else if (input.getType() instanceof IntegerType it) {
            return builder.xor(inputType, llvmInput, intConstant(it.asUnsigned().truncateValue(0xFFFF_FFFF_FFFF_FFFFL))).asLocal();
        } else {
            throw new IllegalStateException();
        }
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

    public LLValue visit(final Void param, final Load node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        org.qbicc.machine.llvm.op.Load loadInsn = builder.load(map(valueHandle.getType()), map(valueHandle.getPointeeType()), ptr);
        loadInsn.align(node.getType().getAlign());
        ReadAccessMode accessMode = node.getAccessMode();
        if (SingleUnshared.includes(accessMode)) {
            // do nothing; not atomic
        } else if (SinglePlain.includes(accessMode)) {
            loadInsn.atomic(OrderingConstraint.unordered);
        } else if (SingleOpaque.includes(accessMode)) {
            loadInsn.atomic(OrderingConstraint.monotonic);
        } else if (SingleAcquire.includes(accessMode)) {
            loadInsn.atomic(OrderingConstraint.acquire);
        } else if (SingleSeqCst.includes(accessMode)) {
            loadInsn.atomic(OrderingConstraint.seq_cst);
        } else {
            throw new IllegalArgumentException("LLVM load does not directly support access mode " + accessMode);
        }
        return loadInsn.asLocal();
    }

    @Override
    public LLValue visit(Void unused, ReadModifyWrite node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        AtomicRmw insn = builder.atomicrmw(map(valueHandle.getType()), map(node.getUpdateValue()), map(node.getUpdateValue().getType()), ptr);
        switch (node.getOp()) {
            case SET -> insn.xchg();
            case ADD -> insn.add();
            case SUB -> insn.sub();
            case BITWISE_AND -> insn.and();
            case BITWISE_NAND -> insn.nand();
            case BITWISE_OR -> insn.or();
            case BITWISE_XOR -> insn.xor();
            case MIN -> insn.min();
            case MAX -> insn.max();
        }
        insn.align(valueHandle.getPointeeType().getAlign());
        insn.ordering(getOC(node.getReadAccessMode().combinedWith(node.getWriteAccessMode())));
        return insn.asLocal();
    }

    public LLValue visit(final Void param, final Neg node) {
        Type javaInputType = node.getInput().getType();
        LLValue inputType = map(javaInputType);
        LLValue llvmInput = map(node.getInput());
        if (isFloating(javaInputType)) {
            return builder.fneg(inputType, llvmInput).asLocal();
        } else {
            return builder.sub(inputType, ZERO, llvmInput).asLocal();
        }
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

    public LLValue visit(final Void param, final BitCast node) {
        ValueType javaInputType = node.getInput().getType();
        ValueType javaOutputType = node.getType();
        LLValue llvmInput = map(node.getInput());
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        if (inputType.equals(outputType)) {
            return llvmInput;
        }

        if (isPointerLike(javaInputType) && isPointerLike(javaOutputType) && isReference(javaInputType) != isReference(javaOutputType)) {
            if (isReference(javaInputType)) {
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
                if (isReference(javaInputType) != isReference(javaOutputType)) {
                    if (isReference(javaInputType)) {
                        return createRefToPtrCast(inputType, llvmInput, outputType);
                    } else {
                        return createPtrToRefCast(inputType, llvmInput, outputType);
                    }
                } else {
                    return builder.bitcast(inputType, llvmInput, outputType).asLocal();
                }
            } else if (javaOutputType instanceof IntegerType) {
                if (isReference(javaInputType)) {
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
                if (isReference(javaOutputType)) {
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
        LLValue llValue = invokeResults.get(node.getInvoke());
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

    public LLValue visit(final Void param, final VaArg node) {
        map(node.getDependency());
        Value vaList = node.getVaList();
        return builder.va_arg(map(vaList.getType()), map(vaList), map(node.getType())).asLocal();
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
        LazyLLValue lazyValue = newLazyValue();
        mappedValues.put(node, lazyValue);
        map(node.getDependency());
        FunctionType functionType = (FunctionType) node.getCalleeType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        ValueHandle valueHandle = node.getValueHandle();
        LLValue llTarget = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        Call call = builder.call(llType, llTarget).noTail();
        LLValue llValue = call.asLocal(schedule.getBlockForNode(node).toString() + '.' + node.getScheduleIndex());
        lazyValue.resolveTo(llValue);
        setCallArguments(call, arguments);
        setCallReturnValue(call, functionType);
        if (functionType.isVariadic() || valueHandle instanceof AsmHandle) {
            call.attribute(FunctionAttributes.gcLeafFunction);
        } else {
            addStatepointId(call, node);
        }
        return llValue;
    }

    @Override
    public LLValue visit(Void param, CallNoSideEffects node) {
        LazyLLValue lazyValue = newLazyValue();
        mappedValues.put(node, lazyValue);
        FunctionType functionType = (FunctionType) node.getCalleeType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        ValueHandle valueHandle = node.getValueHandle();
        LLValue llTarget;
        llTarget = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        Call call = builder.call(llType, llTarget).noTail();
        LLValue llValue = call.asLocal(schedule.getBlockForNode(node).toString() + '.' + node.getScheduleIndex());
        lazyValue.resolveTo(llValue);
        setCallArguments(call, arguments);
        setCallReturnValue(call, functionType);
        if (functionType.isVariadic() || valueHandle instanceof AsmHandle) {
            call.attribute(FunctionAttributes.gcLeafFunction);
        } else {
            addStatepointId(call, node);
        }
        return llValue;
    }

    @Override
    public Instruction visit(Void param, CallNoReturn node) {
        map(node.getDependency());
        FunctionType functionType = (FunctionType) node.getCalleeType();
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
        if (functionType.isVariadic() || valueHandle instanceof AsmHandle) {
            call.attribute(FunctionAttributes.gcLeafFunction);
        } else {
            addStatepointId(call, node);
        }
        builder.unreachable();
        return call;
    }

    @Override
    public Instruction visit(Void param, TailCall node) {
        map(node.getDependency());
        FunctionType functionType = (FunctionType) node.getCalleeType();
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
        if (functionType.isVariadic() || valueHandle instanceof AsmHandle) {
            call.attribute(FunctionAttributes.gcLeafFunction);
        } else {
            addStatepointId(call, node);
        }
        ValueType returnType = node.getCalleeType().getReturnType();
        if (returnType instanceof VoidType) {
            return builder.ret();
        } else {
            return builder.ret(map(returnType), call.asLocal());
        }
    }

    @Override
    public Instruction visit(Void param, Invoke node) {
        map(node.getDependency());
        FunctionType functionType = (FunctionType) node.getCalleeType();
        List<Value> arguments = node.getArguments();
        LLValue llType = map(functionType);
        ValueHandle valueHandle = node.getValueHandle();
        LLValue llTarget = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        if (invokeResults.containsKey(node)) {
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
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        Call call = builder.invoke(llType, llTarget, resume, mapCatch(node.getCatchBlock()));
        invokeResults.put(node, call.asLocal());
        if (postMapResume) {
            postMap(node.getResumeTarget(), resume);
        }
        if (postMapCatch) {
            postMap(node.getCatchBlock(), catch_);
        }
        setCallArguments(call, arguments);
        setCallReturnValue(call, functionType);
        addPersonalityIfNeeded();
        if (functionType.isVariadic() || valueHandle instanceof AsmHandle) {
            call.attribute(FunctionAttributes.gcLeafFunction);
        } else {
            addStatepointId(call, node);
        }
        return call;
    }

    @Override
    public Instruction visit(Void param, InvokeNoReturn node) {
        map(node.getDependency());
        FunctionType functionType = (FunctionType) node.getCalleeType();
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
        if (functionType.isVariadic() || valueHandle instanceof AsmHandle) {
            call.attribute(FunctionAttributes.gcLeafFunction);
        } else {
            addStatepointId(call, node);
        }
        return call;
    }

    @Override
    public Instruction visit(Void param, TailInvoke node) {
        map(node.getDependency());
        FunctionType functionType = (FunctionType) node.getCalleeType();
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
        ValueType returnType = node.getCalleeType().getReturnType();
        if (returnType instanceof VoidType) {
            LLVM.newBuilder(tailTarget).ret();
        } else {
            LLVM.newBuilder(tailTarget).ret(map(returnType), call.asLocal());
        }
        addPersonalityIfNeeded();
        if (functionType.isVariadic() || valueHandle instanceof AsmHandle) {
            call.attribute(FunctionAttributes.gcLeafFunction);
        } else {
            addStatepointId(call, node);
        }
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
            MethodElement personalityFunction = UnwindExceptionStrategy.get(ctxt).getPersonalityMethod();

            Function personality = ctxt.getExactFunction(personalityFunction);
            PointerLiteral literal = ctxt.getLiteralFactory().literalOf(personality.getPointer());
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
        public GetElementPtr visit(LLVMNodeVisitor param, ElementOf node) {
            ValueHandle nextHandle = node.getValueHandle();
            LLValue index = param.map(node.getIndex());
            LLValue indexType = param.map(node.getIndex().getType());
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
            LLValue offset = param.map(node.getOffsetValue());
            LLValue offsetType = param.map(node.getOffsetValue().getType());
            return param.gep(param.map(node.getPointerValue()), node).arg(false, offsetType, offset);
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
        public LLValue visit(LLVMNodeVisitor param, ElementOf node) {
            return node.accept(GET_HANDLE_ELEMENT_POINTER, param).asLocal();
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
            final Value offsetValue = node.getOffsetValue();
            if (offsetValue.getType() instanceof IntegerType it && offsetValue.isDefEq(param.ctxt.getLiteralFactory().literalOf(it, 0))) {
                return param.map(node.getPointerValue());
            } else {
                return node.accept(GET_HANDLE_ELEMENT_POINTER, param).asLocal();
            }
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
        PointerType pointerType = handle.getType();
        ValueType pointeeType = pointerType.getPointeeType();
        return builder.getelementptr(pointeeType instanceof VoidType ? i8 : map(pointeeType), map(pointerType), ptr);
    }

    private OrderingConstraint getOC(AccessMode mode) {
        if (SinglePlain.includes(mode)) {
            return OrderingConstraint.unordered;
        } else if (SingleOpaque.includes(mode)) {
            return OrderingConstraint.monotonic;
        } else if (SingleAcquire.includes(mode)) {
            return OrderingConstraint.acquire;
        } else if (SingleRelease.includes(mode)) {
            return OrderingConstraint.release;
        } else if (SingleAcqRel.includes(mode)) {
            return OrderingConstraint.acq_rel;
        } else if (SingleSeqCst.includes(mode)) {
            return OrderingConstraint.seq_cst;
        }
        throw Assert.unreachableCode();
    }

    @Override
    public LLValue visit(final Void param, final CmpAndSwap node) {
        map(node.getDependency());
        ValueHandle valueHandle = node.getValueHandle();
        LLValue ptrType = map(valueHandle.getType());
        LLValue type = map(valueHandle.getPointeeType());
        LLValue ptr = valueHandle.accept(GET_HANDLE_POINTER_VALUE, this);
        LLValue expect = map(node.getExpectedValue());
        LLValue update = map(node.getUpdateValue());
        ReadAccessMode readMode = node.getReadAccessMode();
        WriteAccessMode writeMode = node.getWriteAccessMode();
        OrderingConstraint successOrdering = getOC(readMode.combinedWith(writeMode));
        OrderingConstraint failureOrdering = getOC(readMode);
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
        if (block == null) {
            // breakpoint
            throw new NullPointerException();
        }
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
        mapped.name(block.toString());
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
        mapped.name(block + ".catch");
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
            if (instruction != null) {
                addLineComment(value, instruction);
            }
            mappedValues.putIfAbsent(value, mapped);
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

    private static <K, V> Map<K, V> newMap(final Object ignored) {
        return new HashMap<>();
    }
}
