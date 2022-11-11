package org.qbicc.graph;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.ByteArrayLiteral;
import org.qbicc.graph.literal.CompoundLiteral;
import org.qbicc.graph.literal.ConstantLiteral;
import org.qbicc.graph.literal.ElementOfLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.PointerLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
import org.qbicc.graph.literal.ValueConvertLiteral;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.graph.schedule.Schedule;
import org.qbicc.type.definition.element.ExecutableElement;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public interface Node {
    /**
     * Get the call site node if this node is part of an inlined function.  The call site node may be of
     * any type, but must refer to the source line, bytecode index, and enclosing element of the call site
     * that was inlined.
     *
     * @return the call site node, or {@code null} if this node was not inlined from another function
     */
    Node getCallSite();

    /**
     * Get the source element.  Literals will have no source element.
     *
     * @return the source element, or {@code null} if there is none
     */
    ExecutableElement getElement();

    int getSourceLine();

    int getBytecodeIndex();

    int getScheduleIndex();

    void setScheduleIndex(int index);

    BasicBlock getScheduledBlock();

    void setScheduledBlock(BasicBlock block);

    default int getValueDependencyCount() {
        return 0;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    default boolean hasValueHandleDependency() {
        return false;
    }

    default ValueHandle getValueHandle() {
        throw new NoSuchElementException();
    }

    StringBuilder toString(StringBuilder b);

    /**
     * A node copier, which uses a visitor chain to allow observation and transformation of the graph nodes as they
     * are copied.
     */
    final class Copier {
        private final BasicBlock entryBlock;
        private final BasicBlockBuilder blockBuilder;
        private final NodeVisitor<Copier, Value, Node, BasicBlock, ValueHandle> nodeVisitor;
        private final Map<BasicBlock, BlockLabel> copiedBlocks = new HashMap<>();
        private final HashMap<Node, Node> copiedNodes = new HashMap<>();
        private final HashMap<Terminator, BasicBlock> copiedTerminators = new HashMap<>();
        private final Queue<BasicBlock> blockQueue = new ArrayDeque<>();
        private final Terminus terminus = new Terminus();
        private final CompilationContext ctxt;
        private final Schedule schedule;

        Copier(BasicBlock entryBlock, BasicBlockBuilder builder, CompilationContext ctxt,
            BiFunction<CompilationContext, NodeVisitor<Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Copier, Value, Node, BasicBlock, ValueHandle>> nodeVisitorFactory
        ) {
            this.entryBlock = entryBlock;
            this.ctxt = ctxt;
            blockBuilder = builder;
            this.schedule = Schedule.forMethod(entryBlock);
            nodeVisitor = nodeVisitorFactory.apply(ctxt, terminus);
        }

        public static BasicBlock execute(BasicBlock entryBlock, BasicBlockBuilder builder, CompilationContext param,
            BiFunction<CompilationContext, NodeVisitor<Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Copier, Value, Node, BasicBlock, ValueHandle>> nodeVisitorFactory
        ) {
            return new Copier(entryBlock, builder, param, nodeVisitorFactory).copyProgram();
        }

        public BasicBlockBuilder getBlockBuilder() {
            return blockBuilder;
        }

        /**
         * Execute this copier on the subprogram that is entered by the given entry block; returns the block copy.
         *
         * @return a (possibly transformed) copy of the entry block
         */
        public BasicBlock copyProgram() {
            BlockLabel entryCopy = copyBlock(entryBlock);
            BasicBlock block;
            while ((block = blockQueue.poll()) != null) {
                // process and map all queued blocks - might enqueue more blocks
                blockBuilder.begin(copiedBlocks.get(block));
                copyScheduledNodes(block);
            }
            return BlockLabel.getTargetOf(entryCopy);
        }

        /**
         * Copy a single block. Called from any visitor that needs to reference a copy of a block from the original
         * program.  The label will not necessarily be resolved until the queue has been fully processed.
         *
         * @param original the original block
         * @return the label of the copied block
         */
        public BlockLabel copyBlock(BasicBlock original) {
            BlockLabel copy = copiedBlocks.get(original);
            if (copy == null) {
                copy = new BlockLabel();
                copiedBlocks.put(original, copy);
                blockQueue.add(original);
            }
            return copy;
        }

        public void copyScheduledNodes(BasicBlock block) {
            try {
                for (Node node : block.getInstructions()) {
                    copyNode(node);
                }
            } catch (BlockEarlyTermination term) {
                copiedTerminators.put(block.getTerminator(), term.getTerminatedBlock());
            }
        }

        public Node copyNode(Node original) {
            if (original instanceof Value) {
                return copyValue((Value) original);
            } else if (original instanceof Action) {
                return copyAction((Action) original);
            } else if (original instanceof ValueHandle) {
                return copyValueHandle((ValueHandle) original);
            } else {
                assert original instanceof Terminator;
                BasicBlock block = copyTerminator((Terminator) original);
                return block.getTerminator();
            }
        }

        public Value copyValue(Value original) {
            Value copy = (Value) copiedNodes.get(original);
            if (copy == null) {
                if (! (original instanceof Unschedulable) && schedule.getBlockForNode(original) == null) {
                    blockBuilder.getContext().warning(blockBuilder.getCurrentElement(), "Converting unscheduled node %s to unreachable()", original.toString());
                    throw new BlockEarlyTermination(blockBuilder.unreachable());
                }
                int oldLine = blockBuilder.setLineNumber(original.getSourceLine());
                int oldBci = blockBuilder.setBytecodeIndex(original.getBytecodeIndex());
                ExecutableElement oldElement = blockBuilder.setCurrentElement(original.getElement());
                Node origCallSite = original.getCallSite();
                Node oldCallSite = origCallSite == null ? blockBuilder.getCallSite() : blockBuilder.setCallSite(copyNode(origCallSite));
                try {
                    copy = original.accept(nodeVisitor, this);
                    copiedNodes.put(original, copy);
                } finally {
                    blockBuilder.setLineNumber(oldLine);
                    blockBuilder.setBytecodeIndex(oldBci);
                    blockBuilder.setCurrentElement(oldElement);
                    blockBuilder.setCallSite(oldCallSite);
                }
            }
            return copy;
        }

        public ValueHandle copyValueHandle(ValueHandle original) {
            ValueHandle copy = (ValueHandle) copiedNodes.get(original);
            if (copy == null) {
                int oldLine = blockBuilder.setLineNumber(original.getSourceLine());
                int oldBci = blockBuilder.setBytecodeIndex(original.getBytecodeIndex());
                ExecutableElement oldElement = blockBuilder.setCurrentElement(original.getElement());
                Node origCallSite = original.getCallSite();
                Node oldCallSite = origCallSite == null ? blockBuilder.getCallSite() : blockBuilder.setCallSite(copyNode(origCallSite));
                try {
                    copy = original.accept(nodeVisitor, this);
                    copiedNodes.put(original, copy);
                } finally {
                    blockBuilder.setLineNumber(oldLine);
                    blockBuilder.setBytecodeIndex(oldBci);
                    blockBuilder.setCurrentElement(oldElement);
                    blockBuilder.setCallSite(oldCallSite);
                }
            }
            return copy;
        }

        public List<Value> copyValues(List<Value> list) {
            if (list.isEmpty()) {
                return List.of();
            }
            Value[] values = new Value[list.size()];
            int i = 0;
            for (Value original : list) {
                values[i++] = copyValue(original);
            }
            return Arrays.asList(values);
        }

        public Map<Slot, Value> copyArguments(final Terminator terminator) {
            Set<Slot> names = terminator.getOutboundArgumentNames();
            if (names.isEmpty()) {
                return Map.of();
            }
            ImmutableMap<Slot, Value> copy = Maps.immutable.empty();
            for (Slot slot : names) {
                int cnt = terminator.getSuccessorCount();
                for (int i = 0; i < cnt; i ++) {
                    if (terminator.getSuccessor(i).getBlockParameter(slot) != null) {
                        // value is used; copy it
                        copy = copy.newWithKeyValue(slot, copyValue(terminator.getOutboundArgument(slot)));
                        break;
                    }
                }
            }
            return copy.castToMap();
        }

        public Node copyAction(Action original) {
            Node copy = copiedNodes.get(original);
            if (copy == null) {
                if (! (original instanceof Unschedulable) && schedule.getBlockForNode(original) == null) {
                    throw new IllegalStateException("Missing schedule for node");
                }
                int oldLine = blockBuilder.setLineNumber(original.getSourceLine());
                int oldBci = blockBuilder.setBytecodeIndex(original.getBytecodeIndex());
                ExecutableElement oldElement = blockBuilder.setCurrentElement(original.getElement());
                Node origCallSite = original.getCallSite();
                Node oldCallSite = origCallSite == null ? blockBuilder.getCallSite() : blockBuilder.setCallSite(copyNode(origCallSite));
                try {
                    copy = original.accept(nodeVisitor, this);
                    copiedNodes.put(original, copy);
                } finally {
                    blockBuilder.setLineNumber(oldLine);
                    blockBuilder.setBytecodeIndex(oldBci);
                    blockBuilder.setCurrentElement(oldElement);
                    blockBuilder.setCallSite(oldCallSite);
                }
            }
            return copy;
        }

        public BasicBlock copyTerminator(Terminator original) {
            BasicBlock basicBlock = copiedTerminators.get(original);
            if (basicBlock == null) {
                if (! (original instanceof Unschedulable) && schedule.getBlockForNode(original) == null) {
                    throw new IllegalStateException("Missing schedule for node");
                }
                // copy the terminator and its dependencies
                int oldLine = blockBuilder.setLineNumber(original.getSourceLine());
                int oldBci = blockBuilder.setBytecodeIndex(original.getBytecodeIndex());
                ExecutableElement oldElement = blockBuilder.setCurrentElement(original.getElement());
                Node origCallSite = original.getCallSite();
                Node oldCallSite = origCallSite == null ? blockBuilder.getCallSite() : blockBuilder.setCallSite(copyNode(origCallSite));
                BasicBlock block;
                try {
                    block = original.accept(nodeVisitor, this);
                } catch (BlockEarlyTermination term) {
                    block = term.getTerminatedBlock();
                } finally {
                    blockBuilder.setLineNumber(oldLine);
                    blockBuilder.setBytecodeIndex(oldBci);
                    blockBuilder.setCurrentElement(oldElement);
                    blockBuilder.setCallSite(oldCallSite);
                }
                copiedTerminators.put(original, block);
                return block;
            }
            return basicBlock;
        }

        static class Terminus implements NodeVisitor<Copier, Value, Node, BasicBlock, ValueHandle> {
            public Node visitUnknown(final Copier param, final Action node) {
                throw Assert.unreachableCode();
            }

            public BasicBlock visitUnknown(final Copier param, final Terminator node) {
                throw Assert.unreachableCode();
            }

            public Value visitUnknown(final Copier param, final Value node) {
                throw Assert.unreachableCode();
            }

            public ValueHandle visitUnknown(Copier param, ValueHandle node) {
                throw Assert.unreachableCode();
            }

            public Node visit(Copier param, BlockEntry node) {
                return param.getBlockBuilder().getBlockEntry();
            }

            public Node visit(Copier param, MonitorEnter node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().monitorEnter(param.copyValue(node.getInstance()));
            }

            public Node visit(Copier param, MonitorExit node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().monitorExit(param.copyValue(node.getInstance()));
            }

            public Node visit(Copier copier, Reachable node) {
                copier.copyNode(node.getDependency());
                return copier.getBlockBuilder().reachable(copier.copyValue(node.getReachableValue()));
            }

            public Node visit(Copier copier, SafePoint node) {
                copier.copyNode(node.getDependency());
                return copier.getBlockBuilder().safePoint();
            }

            public Node visit(Copier param, InitCheck node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().initCheck(node.getInitializerElement(), param.copyValue(node.getInitThunk()));
            }

            public Node visit(Copier param, DebugAddressDeclaration node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().declareDebugAddress(node.getVariable(), param.copyValue(node.getAddress()));
            }

            public Node visit(Copier param, DebugValueDeclaration node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().setDebugValue(node.getVariable(), param.copyValue(node.getValue()));
            }

            public Node visit(Copier param, Fence node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().fence(node.getAccessMode());
            }

            public BasicBlock visit(Copier param, CallNoReturn node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().callNoReturn(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()));
            }

            public BasicBlock visit(Copier param, Goto node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().goto_(param.copyBlock(node.getResumeTarget()), param.copyArguments(node));
            }

            public BasicBlock visit(Copier param, If node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().if_(param.copyValue(node.getCondition()), param.copyBlock(node.getTrueBranch()), param.copyBlock(node.getFalseBranch()), param.copyArguments(node));
            }

            public BasicBlock visit(Copier param, Invoke node) {
                param.copyNode(node.getDependency());
                // special case: seed the copied return value proactively
                Value invoke;
                try {
                    invoke = param.getBlockBuilder().invoke(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()), param.copyBlock(node.getCatchBlock()), param.copyBlock(node.getResumeTarget()), param.copyArguments(node));
                } catch (BlockEarlyTermination bet) {
                    // oops, the invoke got deleted;
                    param.copiedNodes.put(node.getReturnValue(), param.ctxt.getLiteralFactory().undefinedLiteralOfType(node.getReturnValue().getType()));
                    throw bet;
                }
                param.copiedNodes.put(node.getReturnValue(), invoke);
                return param.getBlockBuilder().getTerminatedBlock();
            }

            public BasicBlock visit(Copier param, InvokeNoReturn node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().invokeNoReturn(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()), param.copyBlock(node.getCatchBlock()), param.copyArguments(node));
            }

            public BasicBlock visit(Copier param, Ret node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().ret(param.copyValue(node.getReturnAddressValue()), param.copyArguments(node));
            }

            public BasicBlock visit(Copier param, Switch node) {
                param.copyNode(node.getDependency());
                int cnt = node.getNumberOfValues();
                BlockLabel[] targetsCopy = new BlockLabel[cnt];
                for (int i = 0; i < cnt; i ++) {
                    targetsCopy[i] = param.copyBlock(node.getTargetForIndex(i));
                }
                return param.getBlockBuilder().switch_(param.copyValue(node.getSwitchValue()), node.getValues(), targetsCopy, param.copyBlock(node.getDefaultTarget()), param.copyArguments(node));
            }

            public BasicBlock visit(Copier param, TailCall node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().tailCall(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()));
            }

            public BasicBlock visit(Copier param, Throw node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().throw_(param.copyValue(node.getThrownValue()));
            }

            public BasicBlock visit(Copier param, Unreachable node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().unreachable();
            }

            public BasicBlock visit(Copier param, Return node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().return_(param.copyValue(node.getReturnValue()));
            }

            public Value visit(final Copier param, final Add node) {
                return param.getBlockBuilder().add(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final AddressOf node) {
                return param.getBlockBuilder().addressOf(param.copyValueHandle(node.getValueHandle()));
            }

            public Value visit(final Copier param, final And node) {
                return param.getBlockBuilder().and(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final ArrayLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final BitCast node) {
                return param.getBlockBuilder().bitCast(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(Copier param, BitCastLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final BitReverse node) {
                return param.getBlockBuilder().bitReverse(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final BlockLiteral node) {
                return param.ctxt.getLiteralFactory().literalOf(param.copyBlock(BlockLabel.getTargetOf(node.getBlockLabel())));
            }

            public Value visit(final Copier copier, final BlockParameter node) {
                return copier.getBlockBuilder().addParam(copier.copyBlock(node.getPinnedBlock()), node.getSlot(), node.getType(), node.possibleValuesAreNullable());
            }

            public Value visit(final Copier param, final BooleanLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final ByteArrayLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final ByteSwap node) {
                return param.getBlockBuilder().byteSwap(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final Call node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().call(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()));
            }

            public Value visit(final Copier param, final CallNoSideEffects node) {
                return param.getBlockBuilder().callNoSideEffects(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()));
            }

            public Value visit(final Copier param, final CheckCast node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().checkcast(param.copyValue(node.getInput()), param.copyValue(node.getToType()),
                    param.copyValue(node.getToDimensions()), node.getKind(), node.getExpectedType());
            }

            public Value visit(final Copier param, final ClassOf node) {
                return param.getBlockBuilder().classOf(param.copyValue(node.getInput()), param.copyValue(node.getDimensions()));
            }

            public Value visit(final Copier param, final CountLeadingZeros node) {
                return param.getBlockBuilder().countLeadingZeros(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final CountTrailingZeros node) {
                return param.getBlockBuilder().countTrailingZeros(param.copyValue(node.getInput()));
            }

            public Value visit(Copier param, Cmp node) {
                return param.getBlockBuilder().cmp(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final CmpAndSwap node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().cmpAndSwap(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getExpectedValue()),
                    param.copyValue(node.getUpdateValue()), node.getReadAccessMode(), node.getWriteAccessMode(), node.getStrength());
            }

            public Value visit(Copier param, CmpG node) {
                return param.getBlockBuilder().cmpG(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(Copier param, CmpL node) {
                return param.getBlockBuilder().cmpL(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(Copier param, Comp node) {
                return param.getBlockBuilder().complement(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final CompoundLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final ConstantLiteral node) {
                return node;
            }

            public ValueHandle visit(Copier param, ConstructorElementHandle node) {
                return param.getBlockBuilder().constructorOf(param.copyValue(node.getInstance()), node.getExecutable(), node.getCallSiteDescriptor(), node.getCallSiteType());
            }

            public ValueHandle visit(Copier param, CurrentThread node) {
                return param.getBlockBuilder().currentThread();
            }

            public Value visit(final Copier param, final Convert node) {
                return param.getBlockBuilder().valueConvert(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(Copier copier, DecodeReference node) {
                return copier.getBlockBuilder().decodeReference(copier.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final Div node) {
                return param.getBlockBuilder().divide(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public ValueHandle visit(final Copier param, final AsmHandle node) {
                return param.getBlockBuilder().asm(node.getInstruction(), node.getConstraints(), node.getFlags(), node.getPointeeType());
            }

            public Value visit(Copier copier, ElementOf node) {
                return copier.getBlockBuilder().elementOf(copier.copyValue(node.getArrayPointer()), copier.copyValue(node.getIndex()));
            }

            public Value visit(Copier param, ElementOfLiteral node) {
                return node;
            }

            public ValueHandle visit(Copier param, ExactMethodElementHandle node) {
                return param.getBlockBuilder().exactMethodOf(param.copyValue(node.getInstance()), node.getExecutable(), node.getCallSiteDescriptor(), node.getCallSiteType());
            }

            public ValueHandle visit(Copier param, FunctionElementHandle node) {
                return param.getBlockBuilder().functionOf(node.getExecutable());
            }

            public Value visit(final Copier param, final Extend node) {
                return param.getBlockBuilder().extend(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final ExtractElement node) {
                return param.getBlockBuilder().extractElement(param.copyValue(node.getArrayValue()), param.copyValue(node.getIndex()));
            }

            public Value visit(final Copier param, final ExtractInstanceField node) {
                return param.getBlockBuilder().extractInstanceField(param.copyValue(node.getObjectValue()), node.getFieldElement());
            }

            public Value visit(final Copier param, final ExtractMember node) {
                return param.getBlockBuilder().extractMember(param.copyValue(node.getCompoundValue()), node.getMember());
            }

            public Value visit(final Copier param, final FloatLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final InsertElement node) {
                return param.getBlockBuilder().insertElement(param.copyValue(node.getArrayValue()), param.copyValue(node.getIndex()), param.copyValue(node.getInsertedValue()));
            }

            public Value visit(final Copier param, final InsertMember node) {
                return param.getBlockBuilder().insertMember(param.copyValue(node.getCompoundValue()), node.getMember(), param.copyValue(node.getInsertedValue()));
            }

            public ValueHandle visit(Copier param, GlobalVariable node) {
                return param.getBlockBuilder().globalVariable(node.getVariableElement());
            }

            public Value visit(final Copier param, final InstanceOf node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().instanceOf(param.copyValue(node.getInstance()), node.getCheckType(), node.getCheckDimensions());
            }

            public Value visit(final Copier param, final IntegerLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final IsEq node) {
                return param.getBlockBuilder().isEq(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final IsGe node) {
                return param.getBlockBuilder().isGe(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final IsGt node) {
                return param.getBlockBuilder().isGt(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final IsLe node) {
                return param.getBlockBuilder().isLe(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final IsLt node) {
                return param.getBlockBuilder().isLt(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final IsNe node) {
                return param.getBlockBuilder().isNe(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public ValueHandle visit(Copier param, InstanceFieldOf node) {
                return param.getBlockBuilder().instanceFieldOf(param.copyValueHandle(node.getValueHandle()), node.getVariableElement());
            }

            public ValueHandle visit(Copier param, InterfaceMethodElementHandle node) {
                return param.getBlockBuilder().interfaceMethodOf(param.copyValue(node.getInstance()), node.getExecutable(), node.getCallSiteDescriptor(), node.getCallSiteType());
            }

            public ValueHandle visit(Copier param, VirtualMethodElementHandle node) {
                return param.getBlockBuilder().virtualMethodOf(param.copyValue(node.getInstance()), node.getExecutable(), node.getCallSiteDescriptor(), node.getCallSiteType());
            }

            public Value visit(final Copier param, final Load node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().load(param.copyValueHandle(node.getValueHandle()), node.getAccessMode());
            }

            public ValueHandle visit(Copier param, LocalVariable node) {
                return param.getBlockBuilder().localVariable(node.getVariableElement());
            }

            public Value visit(Copier param, MemberOf node) {
                return param.getBlockBuilder().memberOf(param.copyValue(node.getStructurePointer()), node.getMember());
            }

            public Value visit(final Copier param, final MethodHandleLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final Max node) {
                return param.getBlockBuilder().max(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final MemberSelector node) {
                return param.getBlockBuilder().selectMember(param.copyValueHandle(node.getValueHandle()));
            }

            public Value visit(final Copier param, final Min node) {
                return param.getBlockBuilder().min(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final Mod node) {
                return param.getBlockBuilder().remainder(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final MultiNewArray node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().multiNewArray(node.getArrayType(), param.copyValues(node.getDimensions()));
            }

            public Value visit(final Copier param, final Multiply node) {
                return param.getBlockBuilder().multiply(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final Neg node) {
                return param.getBlockBuilder().negate(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final New node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().new_(node.getClassObjectType(), param.copyValue(node.getTypeId()), param.copyValue(node.getSize()), param.copyValue(node.getAlign()));
            }

            public Value visit(final Copier param, final NewArray node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().newArray(node.getArrayType(), param.copyValue(node.getSize()));
            }

            public Value visit(final Copier param, final NewReferenceArray node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().newReferenceArray(node.getArrayType(), param.copyValue(node.getElemTypeId()),
                    param.copyValue(node.getDimensions()), param.copyValue(node.getSize()));
            }

            public Value visit(final Copier param, final NotNull node) {
                return param.getBlockBuilder().notNull(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final NullLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final ObjectLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final OffsetOfField node) {
                return param.getBlockBuilder().offsetOfField(node.getFieldElement());
            }

            public Value visit(final Copier param, final Or node) {
                return param.getBlockBuilder().or(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final PointerLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final PopCount node) {
                return param.getBlockBuilder().populationCount(param.copyValue(node.getInput()));
            }

            public Value visit(Copier copier, ReadModifyWrite node) {
                return copier.getBlockBuilder().readModifyWrite(copier.copyValueHandle(node.getValueHandle()), node.getOp(), copier.copyValue(node.getUpdateValue()), node.getReadAccessMode(), node.getWriteAccessMode());
            }

            public Value visit(Copier param, ReferenceTo node) {
                return param.getBlockBuilder().referenceTo(param.copyValueHandle(node.getValueHandle()));
            }

            public Value visit(final Copier param, final Rol node) {
                return param.getBlockBuilder().rol(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final Ror node) {
                return param.getBlockBuilder().ror(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public ValueHandle visit(Copier param, PointerHandle node) {
                return param.getBlockBuilder().pointerHandle(param.copyValue(node.getPointerValue()), param.copyValue(node.getOffsetValue()));
            }

            public Value visit(final Copier param, final Select node) {
                return param.getBlockBuilder().select(param.copyValue(node.getCondition()), param.copyValue(node.getTrueValue()), param.copyValue(node.getFalseValue()));
            }

            public Value visit(final Copier param, final Shl node) {
                return param.getBlockBuilder().shl(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final Shr node) {
                return param.getBlockBuilder().shr(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final StackAllocation node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().stackAllocate(node.getType().getPointeeType(), param.copyValue(node.getCount()), param.copyValue(node.getAlign()));
            }

            public ValueHandle visit(Copier param, StaticField node) {
                return param.getBlockBuilder().staticField(node.getVariableElement());
            }

            public ValueHandle visit(Copier param, StaticMethodElementHandle node) {
                return param.getBlockBuilder().staticMethod(node.getExecutable(), node.getCallSiteDescriptor(), node.getCallSiteType());
            }

            public Node visit(final Copier param, final Store node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().store(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getValue()), node.getAccessMode());
            }

            public Value visit(final Copier param, final StringLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final Sub node) {
                return param.getBlockBuilder().sub(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final Truncate node) {
                return param.getBlockBuilder().truncate(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final TypeLiteral node) {
                return node;
            }

            public Value visit(Copier param, ValueConvertLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final UndefinedLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final VaArg node) {
                return param.getBlockBuilder().vaArg(param.copyValue(node.getVaList()), node.getType());
            }

            public ValueHandle visit(final Copier param, final UnsafeHandle node) {
                return param.getBlockBuilder().unsafeHandle(param.copyValueHandle(node.getBase()), param.copyValue(node.getOffset()), node.getOutputType());
            }

            public Value visit(final Copier param, final Xor node) {
                return param.getBlockBuilder().xor(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final ZeroInitializerLiteral node) {
                return node;
            }
        }
    }
}
