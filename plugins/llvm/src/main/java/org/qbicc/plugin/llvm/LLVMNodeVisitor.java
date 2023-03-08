package org.qbicc.plugin.llvm;

import static org.qbicc.graph.atomic.AccessModes.*;
import static org.qbicc.machine.llvm.Types.*;
import static org.qbicc.machine.llvm.Values.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.graph.Action;
import org.qbicc.graph.Add;
import org.qbicc.graph.And;
import org.qbicc.graph.InvocationNode;
import org.qbicc.graph.literal.AsmLiteral;
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
import org.qbicc.graph.DecodeReference;
import org.qbicc.graph.Div;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.Extend;
import org.qbicc.graph.ExtractElement;
import org.qbicc.graph.ExtractMember;
import org.qbicc.graph.Fence;
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
import org.qbicc.graph.OffsetPointer;
import org.qbicc.graph.Or;
import org.qbicc.graph.OrderedNode;
import org.qbicc.graph.Reachable;
import org.qbicc.graph.ReadModifyWrite;
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
import org.qbicc.graph.Terminator;
import org.qbicc.graph.Truncate;
import org.qbicc.graph.Unreachable;
import org.qbicc.graph.Unschedulable;
import org.qbicc.graph.VaArg;
import org.qbicc.graph.Value;
import org.qbicc.graph.WordCastValue;
import org.qbicc.graph.Xor;
import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.graph.schedule.Util;
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
import org.qbicc.machine.llvm.debuginfo.DILocalVariable;
import org.qbicc.machine.llvm.debuginfo.DIOpcode;
import org.qbicc.machine.llvm.debuginfo.MetadataNode;
import org.qbicc.machine.llvm.impl.LLVM;
import org.qbicc.machine.llvm.op.AtomicRmw;
import org.qbicc.machine.llvm.op.Call;
import org.qbicc.machine.llvm.op.GetElementPtr;
import org.qbicc.machine.llvm.op.HasArguments;
import org.qbicc.machine.llvm.op.IndirectBranch;
import org.qbicc.machine.llvm.op.Instruction;
import org.qbicc.machine.llvm.op.OrderingConstraint;
import org.qbicc.machine.llvm.op.Phi;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.plugin.methodinfo.CallSiteInfo;
import org.qbicc.plugin.unwind.UnwindExceptionStrategy;
import org.qbicc.type.ArrayType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FloatType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.Type;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.ParameterElement;

final class LLVMNodeVisitor implements NodeVisitor<Set<Value>, LLValue, Instruction, Instruction> {
    final CompilationContext ctxt;
    final Module module;
    final LLVMModuleDebugInfo debugInfo;
    final LLValue topSubprogram;
    final LLVMModuleNodeVisitor moduleVisitor;
    final Function functionObj;
    final FunctionDefinition func;
    final BasicBlock entryBlock;
    final Map<Value, LLValue> mappedValues = new HashMap<>();
    final Map<Invoke, LLValue> invokeResults = new HashMap<>();
    final Map<Slot, LLValue> entryParameters = new HashMap<>();
    final Map<BasicBlock, LLBasicBlock> mappedBlocks = new HashMap<>();
    final Map<BasicBlock, LLBasicBlock> mappedCatchBlocks = new HashMap<>();
    final Map<BasicBlock, LLBasicBlock> mappedSpResumeBlocks = new HashMap<>();
    final MethodBody methodBody;
    final LLBuilder builder;
    final Map<Node, LLValue> inlineLocations = new HashMap<>();
    final Map<LocalVariableElement, DILocalVariable> localVariables = new HashMap<>();
    final List<InvocationNode> invocationNodes = new ArrayList<>();
    final Map<Invoke, Set<Phi>> invokeResultsToMap = new HashMap<>();
    private final Map<Set<Value>, Set<Value>> cache = new HashMap<>();
    private final boolean opaquePointers;

    private boolean personalityAdded;

    LLVMNodeVisitor(final CompilationContext ctxt, final Module module, final LLVMModuleDebugInfo debugInfo, final LLValue topSubprogram, final LLVMModuleNodeVisitor moduleVisitor, final Function functionObj, final FunctionDefinition func) {
        this.ctxt = ctxt;
        this.module = module;
        this.debugInfo = debugInfo;
        this.topSubprogram = topSubprogram;
        this.moduleVisitor = moduleVisitor;
        this.functionObj = functionObj;
        this.func = func;
        this.methodBody = functionObj.getBody();
        entryBlock = methodBody.getEntryBlock();
        builder = LLBuilder.newBuilder(func.getRootBlock());
        personalityAdded = false;
        opaquePointers = moduleVisitor.opaquePointers;
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
            entryParameters.put(slot, param.asValue());
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
        // list the blocks in order, more or less
        List<BasicBlock> blockList = new ArrayList<>(64);
        findBlocks(entryBlock, blockList, new BitSet());
        blockList.sort(Comparator.comparingInt(BasicBlock::getIndex));
        for (BasicBlock basicBlock : blockList) {
            preMap(basicBlock);
        }
        for (BasicBlock basicBlock : blockList) {
            postMap(basicBlock, preMap(basicBlock));
        }
        for (Map.Entry<Invoke, Set<Phi>> entry : invokeResultsToMap.entrySet()) {
            final Invoke invoke = entry.getKey();
            final Set<Phi> phis = entry.getValue();
            final BasicBlock block = invoke.getTerminatedBlock();
            LLBasicBlock llBasicBlock = mappedSpResumeBlocks.get(block);
            if (llBasicBlock == null) {
                llBasicBlock = map(block);
            }
            final LLValue result = invokeResults.get(invoke);
            for (Phi phi : phis) {
                phi.item(result, llBasicBlock);
            }
        }
    }

    private void findBlocks(final BasicBlock block, final List<BasicBlock> blockList, final BitSet visited) {
        final int index = block.getIndex();
        if (visited.get(index)) {
            return;
        }
        visited.set(index);
        blockList.add(block);
        final Terminator t = block.getTerminator();
        final int cnt = t.getSuccessorCount();
        for (int i = 0; i < cnt; i ++) {
            final BasicBlock successor = t.getSuccessor(i);
            findBlocks(successor, blockList, visited);
        }
    }

    // actions

    public Instruction visit(final Set<Value> liveValues, final BlockEntry node) {
        // no operation
        return null;
    }

    private static final LLValue llvm_dbg_value = Values.global("llvm.dbg.value");
    private static final LLValue emptyExpr = diExpression().asValue();

    @Override
    public Instruction visit(final Set<Value> liveValues, final DebugAddressDeclaration node) {
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
        call.comment("local var " + node.getVariable().getName());
        return call;
    }

    @Override
    public Instruction visit(final Set<Value> liveValues, final DebugValueDeclaration node) {
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
        call.comment("local var " + node.getVariable().getName());
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

    public Instruction visit(final Set<Value> liveValues, final Store node) {
        Value pointer = node.getPointer();
        LLValue ptr = map(pointer);
        org.qbicc.machine.llvm.op.Store storeInsn = builder.store(map(pointer.getType()), map(node.getValue()), map(node.getValue().getType()), ptr);
        storeInsn.align(pointer.getPointeeType().getAlign());
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

    public Instruction visit(final Set<Value> liveValues, final Fence node) {
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

    public Instruction visit(final Set<Value> liveValues, final Reachable node) {
        map(node.getReachableValue());
        // nothing actually emitted
        return null;
    }

    // terminators

    public Instruction visit(final Set<Value> liveValues, final Goto node) {
        return builder.br(map(node.getResumeTarget()));
    }

    public Instruction visit(final Set<Value> liveValues, final If node) {
        return builder.br(map(node.getCondition()), map(node.getTrueBranch()), map(node.getFalseBranch()));
    }

    public Instruction visit(final Set<Value> liveValues, final Ret node) {
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

    public Instruction visit(final Set<Value> liveValues, final Unreachable node) {
        return builder.unreachable();
    }

    public Instruction visit(final Set<Value> liveValues, final Switch node) {
        org.qbicc.machine.llvm.op.Switch switchInst = builder.switch_(i32, map(node.getSwitchValue()), map(node.getDefaultTarget()));

        for (int i = 0; i < node.getNumberOfValues(); i++)
            switchInst.case_(Values.intConstant(node.getValueForIndex(i)), map(node.getTargetForIndex(i)));

        return switchInst;
    }

    public Instruction visit(final Set<Value> liveValues, final Return node) {
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

    public LLValue visit(final Set<Value> liveValues, final Add node) {
        ValueType type = node.getType();
        LLValue inputType = map(type);
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(type) ?
               builder.fadd(inputType, llvmLeft, llvmRight).setLValue(map(node)) :
               builder.add(inputType, llvmLeft, llvmRight).setLValue(map(node));
    }


    public LLValue visit(final Set<Value> liveValues, final And node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return builder.and(map(node.getType()), llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, AsmLiteral node) {
        return Values.asm(node.getInstruction(), node.getConstraints(), map(node.getFlags()));
    }

    @Override
    public LLValue visit(Set<Value> liveValues, BlockParameter node) {
        BasicBlock block = node.getPinnedBlock();
        Slot slot = node.getSlot();
        if (block.getIndex() == 1) {
            return entryParameters.get(slot);
        } else {
            // synthesize a phi for it
            Phi phi = builder.phi(map(node.getType()));
            // pre-seed
            for (BasicBlock incoming : block.getIncoming()) {
                LLBasicBlock mappedIncoming = map(incoming);
                Terminator t = incoming.getTerminator();
                if (!t.isImplicitOutboundArgument(slot, block)) {
                    LLValue mappedVal = map(t.getOutboundArgument(slot));
                    int cnt = t.getSuccessorCount();
                    for (int i = 0; i < cnt; i ++) {
                        if (t.getSuccessor(i) == block) {
                            phi.item(mappedVal, mappedIncoming);
                        }
                    }
                } else if (slot == Slot.result() && t instanceof Invoke inv) {
                    // special case: invocation results
                    invokeResultsToMap.computeIfAbsent(inv, LLVMNodeVisitor::newSet).add(phi);
                }
            }
            return phi.setLValue(map(node));
        }
    }

    private static <E> Set<E> newSet(final Object ignored) {
        return new HashSet<>(4);
    }

    @Override
    public LLValue visit(Set<Value> liveValues, Cmp node) {
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
        ).setLValue(map(node));
    }

    @Override
    public LLValue visit(Set<Value> liveValues, CmpG node) {
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
        ).setLValue(map(node));
    }

    @Override
    public LLValue visit(Set<Value> liveValues, CmpL node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());

        LLValue booleanType = map(ctxt.getTypeSystem().getBooleanType());
        LLValue integerType = map(ctxt.getTypeSystem().getSignedInteger32Type());
        org.qbicc.machine.llvm.op.Select select = builder.select(
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
        );
        return select.setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Comp node) {
        final Value input = node.getInput();
        final LLValue inputType = map(input.getType());
        final LLValue llvmInput = map(input);
        if (input.getType() instanceof BooleanType) {
            return builder.xor(inputType, llvmInput, TRUE).setLValue(map(node));
        } else if (input.getType() instanceof IntegerType it) {
            return builder.xor(inputType, llvmInput, intConstant(it.asUnsigned().truncateValue(0xFFFF_FFFF_FFFF_FFFFL))).setLValue(map(node));
        } else {
            throw new IllegalStateException();
        }
    }

    public LLValue visit(final Set<Value> liveValues, final IsEq node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getLeftInput().getType()) ?
            builder.fcmp(FloatCondition.oeq, inputType, llvmLeft, llvmRight).setLValue(map(node)) :
            builder.icmp(IntCondition.eq, inputType, llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final IsNe node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getLeftInput().getType()) ?
            builder.fcmp(FloatCondition.one, inputType, llvmLeft, llvmRight).setLValue(map(node)) :
            builder.icmp(IntCondition.ne, inputType, llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final IsLt node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
            builder.fcmp(FloatCondition.olt, inputType, llvmLeft, llvmRight).setLValue(map(node)) :
            isSigned(valueType) ?
                builder.icmp(IntCondition.slt, inputType, llvmLeft, llvmRight).setLValue(map(node)) :
                builder.icmp(IntCondition.ult, inputType, llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final IsLe node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
            builder.fcmp(FloatCondition.ole, inputType, llvmLeft, llvmRight).setLValue(map(node)) :
            isSigned(valueType) ?
                builder.icmp(IntCondition.sle, inputType, llvmLeft, llvmRight).setLValue(map(node)) :
                builder.icmp(IntCondition.ule, inputType, llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final IsGt node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
            builder.fcmp(FloatCondition.ogt, inputType, llvmLeft, llvmRight).setLValue(map(node)) :
            isSigned(valueType) ?
                builder.icmp(IntCondition.sgt, inputType, llvmLeft, llvmRight).setLValue(map(node)) :
                builder.icmp(IntCondition.ugt, inputType, llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final IsGe node) {
        Value left = node.getLeftInput();
        LLValue inputType = map(left.getType());
        LLValue llvmLeft = map(left);
        LLValue llvmRight = map(node.getRightInput());
        ValueType valueType = node.getLeftInput().getType();
        return isFloating(valueType) ?
            builder.fcmp(FloatCondition.oge, inputType, llvmLeft, llvmRight).setLValue(map(node)) :
            isSigned(valueType) ?
                builder.icmp(IntCondition.sge, inputType, llvmLeft, llvmRight).setLValue(map(node)) :
                builder.icmp(IntCondition.uge, inputType, llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Or node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return builder.or(map(node.getType()), llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Xor node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return builder.xor(map(node.getType()), llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Multiply node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getType()) ?
               builder.fmul(inputType, llvmLeft, llvmRight).setLValue(map(node)) :
               builder.mul(inputType, llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Select node) {
        Value trueValue = node.getTrueValue();
        LLValue inputType = map(trueValue.getType());
        Value falseValue = node.getFalseValue();
        return builder.select(map(node.getCondition().getType()), map(node.getCondition()), inputType, map(trueValue), map(falseValue)).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Load node) {
        LLValue ptr = map(node.getPointer());
        org.qbicc.machine.llvm.op.Load loadInsn = builder.load(map(node.getPointer().getType()), map(node.getType()), ptr);
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
        return loadInsn.setLValue(map(node));
    }

    @Override
    public LLValue visit(Set<Value> liveValues, ReadModifyWrite node) {
        Value pointer = node.getPointer();
        LLValue ptr = map(pointer);
        AtomicRmw insn = builder.atomicrmw(map(pointer.getType()), map(node.getUpdateValue()), map(node.getUpdateValue().getType()), ptr);
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
        insn.align(pointer.getPointeeType().getAlign());
        insn.ordering(getOC(node.getReadAccessMode().combinedWith(node.getWriteAccessMode())));
        return insn.setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Neg node) {
        Type javaInputType = node.getInput().getType();
        LLValue inputType = map(javaInputType);
        LLValue llvmInput = map(node.getInput());
        if (isFloating(javaInputType)) {
            return builder.fneg(inputType, llvmInput).setLValue(map(node));
        } else {
            return builder.sub(inputType, ZERO, llvmInput).setLValue(map(node));
        }
    }

    public LLValue visit(Set<Value> liveValues, NotNull node) {
        return map(node.getInput());
    }

    public LLValue visit(Set<Value> liveValues, OffsetPointer node) {
        ValueType pointeeType = node.getPointeeType();
        GetElementPtr gep = builder.getelementptr(pointeeType instanceof VoidType ? i8 : map(pointeeType), map(node.getType()), map(node.getBasePointer()));
        gep.arg(false, map(node.getOffset().getType()), map(node.getOffset()));
        return gep.setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Shr node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isSigned(node.getType()) ?
               builder.ashr(inputType, llvmLeft, llvmRight).setLValue(map(node)) :
               builder.lshr(inputType, llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Shl node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return builder.shl(map(node.getType()), llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Sub node) {
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getLeftInput().getType())
            ? builder.fsub(map(node.getType()), llvmLeft, llvmRight).setLValue(map(node))
            : builder.sub(map(node.getType()), llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Div node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getType()) ?
               builder.fdiv(inputType, llvmLeft, llvmRight).setLValue(map(node)) :
                    isSigned(node.getType()) ?
                      builder.sdiv(inputType, llvmLeft, llvmRight).setLValue(map(node)) :
                      builder.udiv(inputType, llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Mod node) {
        LLValue inputType = map(node.getType());
        LLValue llvmLeft = map(node.getLeftInput());
        LLValue llvmRight = map(node.getRightInput());
        return isFloating(node.getType()) ?
               builder.frem(inputType, llvmLeft, llvmRight).setLValue(map(node)) :
                    isSigned(node.getType()) ?
                      builder.srem(inputType, llvmLeft, llvmRight).setLValue(map(node)) :
                      builder.urem(inputType, llvmLeft, llvmRight).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final BitCast node) {
        ValueType javaInputType = node.getInput().getType();
        ValueType javaOutputType = node.getType();
        LLValue llvmInput = map(node.getInput());
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        if (inputType.equals(outputType)) {
            return null;
        }
        return builder.bitcast(inputType, llvmInput, outputType).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Convert node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        LLValue llvmInput = map(node.getInput());
        if (inputType.equals(outputType)) {
            return null;
        }

        if (javaInputType instanceof PointerType) {
            if (javaOutputType instanceof ReferenceType) {
                return switch (moduleVisitor.config.getReferenceStrategy()) {
                    case POINTER -> null;
                    case POINTER_AS1 -> builder.addrspacecast(inputType, llvmInput, outputType).setLValue(map(node));
                };
            } else if (javaOutputType instanceof IntegerType) {
                return builder.ptrtoint(inputType, llvmInput, outputType).setLValue(map(node));
            }
            // else invalid
        } else if (javaInputType instanceof ReferenceType) {
            if (javaOutputType instanceof PointerType) {
                return switch (moduleVisitor.config.getReferenceStrategy()) {
                    case POINTER -> null;
                    case POINTER_AS1 -> builder.addrspacecast(inputType, llvmInput, outputType).setLValue(map(node));
                };
            } else if (javaOutputType instanceof IntegerType) {
                return builder.ptrtoint(inputType, llvmInput, outputType).setLValue(map(node));
            }
            // else invalid
        } else if (javaInputType instanceof FloatType) {
            if (javaOutputType instanceof SignedIntegerType) {
                return builder.fptosi(inputType, llvmInput, outputType).setLValue(map(node));
            } else if (javaOutputType instanceof UnsignedIntegerType) {
                return builder.fptoui(inputType, llvmInput, outputType).setLValue(map(node));
            }
            // else invalid
        } else if (javaInputType instanceof IntegerType) {
            if (javaOutputType instanceof PointerType || javaOutputType instanceof ReferenceType) {
                return builder.inttoptr(inputType, llvmInput, outputType).setLValue(map(node));
            } else if (javaInputType instanceof SignedIntegerType) {
                if (javaOutputType instanceof FloatType) {
                    return builder.sitofp(inputType, llvmInput, outputType).setLValue(map(node));
                }
            } else if (javaInputType instanceof UnsignedIntegerType) {
                if (javaOutputType instanceof FloatType) {
                    return builder.uitofp(inputType, llvmInput, outputType).setLValue(map(node));
                }
            }
            // else invalid
        }

        ctxt.error(functionObj.getOriginalElement(), node, "llvm: Unhandled conversion %s -> %s", javaInputType.toString(), javaOutputType.toString());
        return llvmInput;
    }

    public LLValue visit(Set<Value> liveValues, DecodeReference node) {
        final Value input = node.getInput();
        return switch (moduleVisitor.config.getReferenceStrategy()) {
            case POINTER -> null;
            case POINTER_AS1 -> builder.addrspacecast(map(input.getType()), map(input), map(node.getType())).setLValue(map(node));
        };
    }

    public LLValue visit(final Set<Value> liveValues, final Extend node) {
        WordType javaInputType = (WordType) node.getInput().getType();
        WordType javaOutputType = node.getType();
        LLValue llvmInput = map(node.getInput());
        if (javaInputType instanceof IntegerType && javaOutputType instanceof IntegerType && javaInputType.getMinBits() == javaOutputType.getMinBits()) {
            return null;
        }
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        return isFloating(javaInputType) ?
               builder.fpext(inputType, llvmInput, outputType).setLValue(map(node)) :
                    isSigned(javaInputType) ?
                    builder.sext(inputType, llvmInput, outputType).setLValue(map(node)) :
                    builder.zext(inputType, llvmInput, outputType).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final ExtractElement node) {
        LLValue arrayType = map(node.getArrayType());
        LLValue array = map(node.getArrayValue());
        LLValue index = map(node.getIndex());
        return builder.extractvalue(arrayType, array).arg(index).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final ExtractMember node) {
        LLValue compType = map(node.getCompoundType());
        LLValue comp = map(node.getCompoundValue());
        LLValue index = map(node.getCompoundType(), node.getMember());
        return builder.extractvalue(compType, comp).arg(index).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final InsertElement node) {
        LLValue arrayType = map(node.getType());
        LLValue array = map(node.getArrayValue());
        LLValue valueType = map(node.getInsertedValue().getType());
        LLValue value = map(node.getInsertedValue());
        LLValue index = map(node.getIndex());
        return builder.insertvalue(arrayType, array, valueType, value).arg(index).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final InsertMember node) {
        LLValue compType = map(node.getType());
        LLValue comp = map(node.getCompoundValue());
        LLValue valueType = map(node.getInsertedValue().getType());
        LLValue value = map(node.getInsertedValue());
        LLValue index = map(node.getType(), node.getMember());
        return builder.insertvalue(compType, comp, valueType, value).arg(index).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Invoke.ReturnValue node) {
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

    public LLValue visit(final Set<Value> liveValues, final CheckCast node) {
        return map(node.getInput());
    }

    public LLValue visit(final Set<Value> liveValues, final ElementOf node) {
        Value arrayPointer = node.getArrayPointer();
        ArrayType arrayType = arrayPointer.getPointeeType(ArrayType.class);
        PointerType pointerType = arrayType.getPointer();
        LLValue ptr = map(arrayPointer);
        GetElementPtr gep = builder.getelementptr(map(arrayType), map(pointerType), ptr);
        gep.arg(false, i32, ZERO).arg(false, map(node.getIndex().getType()), map(node.getIndex()));
        return gep.setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final MemberOf node) {
        CompoundType structType = node.getStructType();
        PointerType pointerType = structType.getPointer();
        LLValue ptr = map(node.getStructurePointer());
        GetElementPtr gep = builder.getelementptr(map(structType), map(pointerType), ptr);
        CompoundType.Member member = node.getMember();
        gep.arg(false, i32, ZERO).arg(false, i32, map(structType, member));
        gep.comment("member " + member.getName());
        return gep.setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final Truncate node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        LLValue inputType = map(javaInputType);
        LLValue outputType = map(javaOutputType);
        LLValue llvmInput = map(node.getInput());
        return isFloating(javaInputType) ?
               builder.ftrunc(inputType, llvmInput, outputType).setLValue(map(node)) :
               builder.trunc(inputType, llvmInput, outputType).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final VaArg node) {
        Value vaList = node.getVaList();
        return builder.va_arg(map(vaList.getType()), map(vaList), map(node.getType())).setLValue(map(node));
    }

    public LLValue visit(final Set<Value> liveValues, final StackAllocation node) {
        LLValue pointeeType = map(node.getType().getPointeeType());
        LLValue countType = map(node.getCount().getType());
        LLValue count = map(node.getCount());
        LLValue alignment = map(node.getAlign());
        return builder.alloca(pointeeType).elements(countType, count).align(alignment).setLValue(map(node));
    }

    // calls

    @Override
    public LLValue visit(Set<Value> liveValues, org.qbicc.graph.Call node) {
        final Call call = setUpCall(liveValues, node, (type, function) -> builder.call(type, function).noTail(), builder::call);
        return call.setLValue(map(node));
    }

    @Override
    public LLValue visit(Set<Value> liveValues, CallNoSideEffects node) {
        final Call call = setUpCall(liveValues, node, (type, function) -> builder.call(type, function).noTail(), builder::call);
        return call.setLValue(map(node));
    }

    @Override
    public Instruction visit(Set<Value> liveValues, CallNoReturn node) {
        setUpCall(liveValues, node, (type, function) -> builder.call(type, function).attribute(FunctionAttributes.noreturn), null);
        return builder.unreachable();
    }

    @Override
    public Instruction visit(Set<Value> liveValues, TailCall node) {
        final Call call = setUpCall(liveValues, node, (type, function) -> builder.call(type, function).tail(), builder::call);
        ValueType returnType = node.getCalleeType().getReturnType();
        if (returnType instanceof VoidType) {
            return builder.ret();
        } else {
            return builder.ret(map(returnType), call.asLocal());
        }
    }

    @Override
    public Instruction visit(Set<Value> liveValues, Invoke node) {
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
        LLBasicBlock finalResume = resume;
        final Call call = setUpCall(liveValues, node, (llType, llTarget) -> {
            // statepoint
            if (node.isVoidCall()) {
                return builder.invoke(llType, llTarget, finalResume, mapCatch(node.getCatchBlock())).noTail();
            } else {
                return builder.invoke(llType, llTarget, mapSpResume(node.getTerminatedBlock()), mapCatch(node.getCatchBlock())).noTail();
            }
        }, (llType, llTarget) -> {
            // no statepoint
            final Call invoke = builder.invoke(llType, llTarget, finalResume, mapCatch(node.getCatchBlock())).noTail();
            if (! node.isVoidCall()) {
                invokeResults.put(node, invoke.setLValue(map(node.getReturnValue())));
            }
            return invoke;
        }, (llType, llTarget) -> {
            // statepoint result
            final LLBasicBlock spResume = mapSpResume(node.getTerminatedBlock());
            final LLBasicBlock old = builder.moveToBlock(spResume);
            try {
                final Call resultCall = builder.call(llType, llTarget);
                if (! node.isVoidCall()) {
                    invokeResults.put(node, resultCall.setLValue(map(node.getReturnValue())));
                }
                builder.br(finalResume);
                return resultCall;
            } finally {
                builder.moveToBlock(old);
            }
        });
        if (postMapResume) {
            postMap(node.getResumeTarget(), resume);
        }
        if (postMapCatch) {
            postMap(node.getCatchBlock(), catch_);
        }
        addPersonalityIfNeeded();
        return call;
    }

    private LLBasicBlock mapSpResume(final BasicBlock invokeBlock) {
        LLBasicBlock mapped = mappedSpResumeBlocks.get(invokeBlock);
        if (mapped != null) {
            return mapped;
        }
        mapped = func.createBlock();
        mapped.name(invokeBlock + ".resume");
        mappedSpResumeBlocks.put(invokeBlock, mapped);
        return mapped;
    }

    @Override
    public Instruction visit(Set<Value> liveValues, InvokeNoReturn node) {
        LLBasicBlock unreachableTarget = func.createBlock();
        LLVM.newBuilder(unreachableTarget).unreachable();
        LLBasicBlock catch_ = checkMap(node.getCatchBlock());
        boolean postMapCatch = catch_ == null;
        if (postMapCatch) {
            catch_ = preMap(node.getCatchBlock());
        }
        final Call call = setUpCall(liveValues, node, (llType, llTarget) -> builder.invoke(llType, llTarget, unreachableTarget, mapCatch(node.getCatchBlock())).noTail().attribute(FunctionAttributes.noreturn), null);
        if (postMapCatch) {
            postMap(node.getCatchBlock(), catch_);
        }
        addPersonalityIfNeeded();
        return call;
    }

    private Call setUpCall(Set<Value> liveValues, InvocationNode node, BiFunction<LLValue, LLValue, Call> callMaker, BiFunction<LLValue, LLValue, Call> resultCallMaker) {
        return setUpCall(liveValues, node, callMaker, callMaker, resultCallMaker);
    }

    private Call setUpCall(Set<Value> liveValues, InvocationNode node, BiFunction<LLValue, LLValue, Call> spCallMaker, BiFunction<LLValue, LLValue, Call> noSpCallMaker, BiFunction<LLValue, LLValue, Call> spResultCallMaker) {
        FunctionType functionType = (FunctionType) node.getCalleeType();
        List<Value> arguments = node.getArguments();
        // two scans - once to populate the maps, and then once to emit the call in the right order
        preMapArgumentList(arguments);
        Value target = node.getTarget();
        LLValue llTarget = map(target);
        boolean needsStatepoint;
        String statepointReason;
        final ExecutableElement origElement = functionObj.getOriginalElement();
        final boolean callerIsHidden = origElement != null && origElement.hasAllModifiersOf(ClassFile.I_ACC_HIDDEN);
        if (! moduleVisitor.config.isStatepointEnabled()) {
            needsStatepoint = false;
            statepointReason = "Statepoint is disabled";
        } else if (callerIsHidden && functionObj.isNoSafePoints()) {
            // caller is hidden, caller is noSafePoints, so no stack walker will see this call and no safepoint is possible
            needsStatepoint = false;
            statepointReason = "Caller is hidden and noSafePoints";
        } else if (callerIsHidden && target.isNoSafePoints()) {
            // caller is hidden, callee is noSafePoints, so no stack walker will see this call and no safepoint is possible
            needsStatepoint = false;
            statepointReason = "Caller is hidden and callee is noSafePoints";
        } else if (callerIsHidden && liveValues.isEmpty()) {
            // caller is hidden; GC can see this call but there are no live values to relocate
            needsStatepoint = false;
            statepointReason = "Caller is hidden and no live values";
        } else if (target.getPointeeType(InvokableType.class).isVariadic()) {
            // state point is forbidden!
            needsStatepoint = false;
            statepointReason = "Statepoint forbidden (variadic)";
        } else if (target instanceof ProgramObjectLiteral pol && pol.getProgramObject() instanceof FunctionDeclaration fd && fd.getOriginalElement() == null) {
            needsStatepoint = false;
            statepointReason = "Statepoint forbidden (external function)";
        } else if (target instanceof AsmLiteral) {
            needsStatepoint = false;
            statepointReason = "Statepoint forbidden (inline assembly)";
        } else if (functionObj.isNoSafePoints() || target.isNoSafePoints() || liveValues.isEmpty()) {
            // stack walkers can see us but GC is impossible; we do not need live values
            liveValues = Set.of();
            needsStatepoint = true;
            statepointReason = "Visible to stack walk (no live GC values)";
        } else {
            // need statepoint with call info *and* live values
            needsStatepoint = true;
            statepointReason = "Visible to stack walk with live GC values";
        }
        if (needsStatepoint) {
            // wrap call with statepoint
            LLValue statepointDecl = moduleVisitor.generateStatepointDecl(functionType);
            LLValue statepointType = moduleVisitor.mapStatepointType(functionType);
            Call spCall = spCallMaker.apply(statepointType, statepointDecl);
            spCall.comment(statepointReason);
            // record the statepoint so that we can correlate the stack map info back to nodes
            int statepointId = LLVM.getNextStatepointId();
            CallSiteInfo.get(ctxt).mapStatepointIdToNode(statepointId, node);
            invocationNodes.add(node);
            spCall.arg(i64, intConstant(statepointId));
            spCall.arg(i32, ZERO);
            final HasArguments.Argument argument = spCall.arg(map(functionType.getPointer()), llTarget);
            if (moduleVisitor.generator.getLlvmMajor() >= 15) {
                argument.attribute(ParameterAttributes.elementtype(map(functionType)));
            }
            spCall.arg(i32, intConstant(arguments.size()));
            spCall.arg(i32, ONE);
            setCallArguments(spCall, arguments);
            spCall.arg(i64, ZERO);
            spCall.arg(i64, ZERO);
            if (liveValues.size() > 0) {
                HasArguments opBundle = spCall.operandBundle("gc-live");
                for (Value liveValue : liveValues) {
                    opBundle.arg(map(liveValue.getType()), map(liveValue));
                }
            }
            if (spResultCallMaker == null || functionType.getReturnType() instanceof VoidType) {
                return spCall;
            }
            final LLValue resultToken = spCall.asLocal();
            LLValue resultDecl = moduleVisitor.generateStatepointResultDecl(functionType.getReturnType());
            LLValue resultDeclType = moduleVisitor.generateStatepointResultDeclType(functionType.getReturnType());
            Call resultCall = spResultCallMaker.apply(resultDeclType, resultDecl);
            resultCall.arg(token, resultToken);
            return resultCall;
        } else {
            // no live values, or a no-safepoints method
            Call call = noSpCallMaker.apply(map(functionType), llTarget);
            call.comment(statepointReason);
            setCallArguments(call, arguments);
            setCallReturnValue(call, functionType);
            return call;
        }
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

    private void setCallArguments(final HasArguments call, final List<Value> arguments) {
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
            ProgramObjectLiteral literal = ctxt.getLiteralFactory().literalOf(personality);
            // clang generates the personality argument like this (by casting the function to i8* using bitcast):
            //      define dso_local void @_Z7catchitv() #0 personality i8* bitcast (i32 (...)* @__gxx_personality_v0 to i8*) {
            // We can also generate it this way using following construct:
            //      func.personality(Values.bitcastConstant(map(literal), map(literal.getType()), ptrTo(i8)), ptrTo(i8));
            // but directly specifying the function works as well.
            func.personality(map(literal), map(literal.getType()));
            personalityAdded = true;
        }
    }

    private static Set<AsmFlag> map(final Set<AsmLiteral.Flag> flags) {
        EnumSet<AsmFlag> output = EnumSet.noneOf(AsmFlag.class);
        if (flags.contains(AsmLiteral.Flag.SIDE_EFFECT)) {
            output.add(AsmFlag.SIDE_EFFECT);
        }
        if (! flags.contains(AsmLiteral.Flag.NO_THROW)) {
            output.add(AsmFlag.UNWIND);
        }
        if (flags.contains(AsmLiteral.Flag.ALIGN_STACK)) {
            output.add(AsmFlag.ALIGN_STACK);
        }
        if (flags.contains(AsmLiteral.Flag.INTEL_DIALECT)) {
            output.add(AsmFlag.INTEL_DIALECT);
        }
        return output;
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
    public LLValue visit(final Set<Value> liveValues, final CmpAndSwap node) {
        Value pointerValue = node.getPointer();
        LLValue ptrType = map(pointerValue.getType());
        LLValue type = map(pointerValue.getPointeeType());
        LLValue ptr = map(pointerValue);
        LLValue expect = map(node.getExpectedValue());
        LLValue update = map(node.getUpdateValue());
        ReadAccessMode readMode = node.getReadAccessMode();
        WriteAccessMode writeMode = node.getWriteAccessMode();
        OrderingConstraint successOrdering = getOC(readMode.combinedWith(writeMode));
        OrderingConstraint failureOrdering = getOC(readMode);
        if (failureOrdering == OrderingConstraint.unordered) {
            failureOrdering = OrderingConstraint.monotonic; // LLVM requires failure mode to be at least monotonic
        }
        org.qbicc.machine.llvm.op.CmpAndSwap cmpAndSwapBuilder = builder.cmpAndSwap(
            ptrType, type, ptr, expect, update, successOrdering, failureOrdering);
        if (node.getStrength() == CmpAndSwap.Strength.WEAK) {
            cmpAndSwapBuilder.weak();
        }
        return cmpAndSwapBuilder.setLValue(map(node));
    }

    // unknown node catch-all methods

    public LLValue visitUnknown(final Set<Value> liveValues, final Value node) {
        ctxt.error(Location.builder().setNode(node).build(), "llvm: Unrecognized value %s", node.getClass());
        return FALSE;
    }

    @Override
    public LLValue visitAny(final Set<Value> liveValues, Literal literal) {
        return literal.accept(moduleVisitor, null);
    }

    public Instruction visitUnknown(final Set<Value> liveValues, final Action node) {
        ctxt.error(functionObj.getOriginalElement(), node, "llvm: Unrecognized action %s", node.getClass());
        return null;
    }

    public Instruction visitUnknown(final Set<Value> liveValues, final Terminator node) {
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
        LLBasicBlock oldBuilderBlock = builder.moveToBlock(mapped);
        LLValue oldBuilderDebugLocation = builder.getDebugLocation();
        final List<Node> insnList = block.getInstructions();
        boolean maySafePoint = ! this.functionObj.isNoSafePoints();
        mapInstructions(block, maySafePoint ? new HashSet<>(block.getLiveOuts()) : null, insnList.listIterator(insnList.size()), maySafePoint);
        builder.setDebugLocation(oldBuilderDebugLocation);
        builder.moveToBlock(oldBuilderBlock);

        return mapped;
    }

    /**
     * Walk the instruction list backwards and then forwards, tracking live values for each node and then emitting them.
     *
     * @param block the block being mapped (must not be {@code null})
     * @param liveSet the current instruction live-out set (must not be {@code null})
     * @param iter the list iterator for the block, starting at the end (must not be {@code null})
     * @param maySafePoint {@code true} if the block is in an element that may safepoint, or {@code false} otherwise
     */
    private void mapInstructions(BasicBlock block, HashSet<Value> liveSet, ListIterator<Node> iter, boolean maySafePoint) {
        if (! iter.hasPrevious()) {
            // all done
            return;
        }
        // process current instruction
        final Node node = iter.previous();
        final Set<Value> live;
        if (maySafePoint) {
            if (node instanceof Value v && ! (v instanceof BlockParameter bp && bp.getPinnedBlock() == block)) {
                // this is where it was defined, thus we can remove it from the live set.
                // exception: block params are live all the way to the start of the block.
                liveSet.remove(v);
            } else if (node instanceof Invoke inv) {
                // special case!
                liveSet.remove(inv.getReturnValue());
            }
            if (node instanceof OrderedNode on && on.maySafePoint() && ! liveSet.isEmpty()) {
                live = Util.getCachedSet(cache, liveSet);
            } else {
                live = Set.of();
            }
            final int cnt = node.getValueDependencyCount();
            for (int i = 0; i < cnt; i ++) {
                final Value val = node.getValueDependency(i);
                if (val.getType() instanceof ReferenceType && ! (val instanceof Literal)) {
                    liveSet.add(val);
                }
            }
        } else {
            // no need to track live values
            live = Set.of();
        }
        // process and emit previous instruction (if any)
        mapInstructions(block, liveSet, iter, maySafePoint);
        // now emit current instruction
        builder.setDebugLocation(dbg(node));
        Instruction instruction;
        if (node instanceof Terminator t) {
            instruction = t.accept(this, live);
        } else if (node instanceof Value value) {
            LLValue llValue = value.accept(this, live);
            instruction = llValue == null ? null : llValue.getInstruction();
        } else if (node instanceof Action action) {
            instruction = action.accept(this, live);
        } else {
            throw new IllegalStateException();
        }
        if (instruction != null) {
            addLineComment(node, instruction);
        }
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
        mappedCatchBlocks.put(block, mapped);
        return mapped;
    }

    private LLValue map(Type type) {
        return moduleVisitor.map(type);
    }

    private LLValue map(Value value) {
        if (value instanceof Unschedulable) {
            // emit every time
            return value.accept(this, null);
        }
        // special cases for every node which does not end up in the final program
        // todo: we should eliminate these in org.qbicc.plugin.llvm.LLVMCompatibleBasicBlockBuilder
        if (value instanceof NotNull nn) {
            // special handling of constraint
            return map(nn.getInput());
        } else if (value instanceof WordCastValue wcv && map(wcv.getType()).equals(map(wcv.getInput().getType()))) {
            // no node generated
            return map(wcv.getInput());
        } else if (value instanceof WordCastValue wcv && wcv.getType() instanceof IntegerType out && wcv.getInput().getType() instanceof IntegerType in && out.getMinBits() == in.getMinBits()) {
            // no node generated for signedness casts
            return map(wcv.getInput());
        } else if (opaquePointers && value instanceof BitCast bc &&
            (bc.getType() instanceof PointerType && bc.getInput().getType() instanceof PointerType ||
             bc.getType() instanceof ReferenceType && bc.getInput().getType() instanceof ReferenceType)
        ) {
            // all pointers are the same
            return map(bc.getInput());
        } else if (moduleVisitor.config.getReferenceStrategy() == ReferenceStrategy.POINTER) {
            // exclude nodes which convert between pointers and references
            if (value instanceof DecodeReference dr) {
                return map(dr.getInput());
            } else if (value instanceof Convert conv) {
                if (conv.getInput().getType() instanceof PointerType && conv.getType() instanceof ReferenceType) {
                    return map(conv.getInput());
                } else if (conv.getInput().getType() instanceof ReferenceType && conv.getType() instanceof PointerType) {
                    return map(conv.getInput());
                }
            }
        }
        LLValue mapped = mappedValues.get(value);
        if (mapped != null) {
            return mapped;
        }
        String name;
        BasicBlock block;
        if (value instanceof Invoke.ReturnValue rv) {
            // special schedule behavior
            Invoke invoke = rv.getInvoke();
            block = invoke.getTerminatedBlock();
            int scheduleIndex = invoke.getScheduleIndex();
            if (scheduleIndex == -1) {
                throw new IllegalStateException();
            }
            name = block.toString(new StringBuilder()).append('.').append(scheduleIndex).toString();
        } else {
            block = value.getScheduledBlock();
            if (value instanceof BlockParameter bp) {
                if (bp.getPinnedBlock().getIndex() == 1) {
                    // special, special case
                    mapped = entryParameters.get(bp.getSlot());
                    mappedValues.put(value, mapped);
                    return mapped;
                } else {
                    // special case
                    name = block.toString(new StringBuilder()).append('.').append(bp.getSlot()).toString();
                }
            } else {
                int scheduleIndex = value.getScheduleIndex();
                if (scheduleIndex == -1) {
                    throw new IllegalStateException();
                }
                name = block.toString(new StringBuilder()).append('.').append(scheduleIndex).toString();
            }
        }
        mapped = Values.local(name);
        mappedValues.put(value, mapped);
        return mapped;
    }

    private void addLineComment(final Node node, final Instruction instruction) {
        if (instruction != null) {
            instruction.comment(node.getElement().getSourceFileName() + ":" + node.getSourceLine() + " bci@" + node.getBytecodeIndex());
        }
    }

    private LLValue map(CompoundType compoundType, CompoundType.Member member) {
        return moduleVisitor.map(compoundType, member);
    }
}
