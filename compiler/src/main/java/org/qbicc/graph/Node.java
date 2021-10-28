package org.qbicc.graph;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.BiFunction;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.graph.literal.BooleanLiteral;
import org.qbicc.graph.literal.CompoundLiteral;
import org.qbicc.graph.literal.ConstantLiteral;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.MethodHandleLiteral;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.graph.literal.UndefinedLiteral;
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
        private final Queue<PhiValue> phiQueue = new ArrayDeque<>();
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
                // process and map all queued blocks - might enqueue more blocks or phis
                blockBuilder.begin(copiedBlocks.get(block));
                copyScheduledNodes(block);
            }
            // now process all phis (all blocks will have been enqueued)
            PhiValue orig;
            while ((orig = phiQueue.poll()) != null) {
                PhiValue copy = (PhiValue) copiedNodes.get(orig);
                BasicBlock ourBlock = copy.getPinnedBlock();
                // process and map all incoming values - might enqueue more blocks or phis
                for (BasicBlock incomingBlock : orig.getPinnedBlock().getIncoming()) {
                    Terminator incomingTerminator = incomingBlock.getTerminator();
                    if (incomingBlock.isReachable()) {
                        Value val = orig.getValueForInput(incomingTerminator);
                        if (val != null) {
                            BasicBlock copiedIncomingBlock = copiedTerminators.get(incomingTerminator);
                            // if this block is null, that means that the copied block can no longer flow into this block due to transformation
                            if (copiedIncomingBlock != null && copiedIncomingBlock.isSucceededBy(ourBlock)) {
                                copy.setValueForBlock(ctxt, blockBuilder.getCurrentElement(), copiedIncomingBlock, copyValue(val));
                            }
                        }
                    }
                }
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
                // copy all nodes except the terminator which should be the last one to be copied
                for (Node node: schedule.getNodesForBlock(block)) {
                    if (!(node instanceof Terminator)) {
                        copyNode(node);
                    }
                }
                copyTerminator(block.getTerminator());
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

        public PhiValue enqueue(PhiValue originalPhi) {
            phiQueue.add(Assert.checkNotNullParam("originalPhi", originalPhi));
            return originalPhi;
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

            public Node visit(Copier param, ClassInitCheck node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().classInitCheck(node.getObjectType());
            }

            public Node visit(Copier param, DebugAddressDeclaration node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().declareDebugAddress(node.getVariable(), param.copyValue(node.getAddress()));
            }

            public Node visit(Copier param, Fence node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().fence(node.getAtomicityMode());
            }

            public BasicBlock visit(Copier param, CallNoReturn node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().callNoReturn(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()));
            }

            public BasicBlock visit(Copier param, Goto node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().goto_(param.copyBlock(node.getResumeTarget()));
            }

            public BasicBlock visit(Copier param, If node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().if_(param.copyValue(node.getCondition()), param.copyBlock(node.getTrueBranch()), param.copyBlock(node.getFalseBranch()));
            }

            public BasicBlock visit(Copier param, Invoke node) {
                param.copyNode(node.getDependency());
                // special case: seed the copied return value proactively
                Value invoke;
                try {
                    invoke = param.getBlockBuilder().invoke(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()), param.copyBlock(node.getCatchBlock()), param.copyBlock(node.getResumeTarget()));
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
                return param.getBlockBuilder().invokeNoReturn(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()), param.copyBlock(node.getCatchBlock()));
            }

            public BasicBlock visit(Copier param, Jsr node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().jsr(param.copyBlock(node.getResumeTarget()), (BlockLiteral) param.copyValue(node.getReturnAddressValue()));
            }

            public BasicBlock visit(Copier param, Ret node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().ret(param.copyValue(node.getReturnAddressValue()));
            }

            public BasicBlock visit(Copier param, Return node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().return_();
            }

            public BasicBlock visit(Copier param, Switch node) {
                param.copyNode(node.getDependency());
                int cnt = node.getNumberOfValues();
                BlockLabel[] targetsCopy = new BlockLabel[cnt];
                for (int i = 0; i < cnt; i ++) {
                    targetsCopy[i] = param.copyBlock(node.getTargetForIndex(i));
                }
                return param.getBlockBuilder().switch_(param.copyValue(node.getSwitchValue()), node.getValues(), targetsCopy, param.copyBlock(node.getDefaultTarget()));
            }

            public BasicBlock visit(Copier param, TailCall node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().tailCall(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()));
            }

            public BasicBlock visit(Copier param, TailInvoke node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().tailInvoke(param.copyValueHandle(node.getValueHandle()), param.copyValues(node.getArguments()), param.copyBlock(node.getCatchBlock()));
            }

            public BasicBlock visit(Copier param, Throw node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().throw_(param.copyValue(node.getThrownValue()));
            }

            public BasicBlock visit(Copier param, Unreachable node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().unreachable();
            }

            public BasicBlock visit(Copier param, ValueReturn node) {
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

            public Value visit(final Copier param, final ArrayLength node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().arrayLength(param.copyValueHandle(node.getInstance()));
            }

            public Value visit(final Copier param, final ArrayLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final BitCast node) {
                return param.getBlockBuilder().bitCast(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final BitReverse node) {
                return param.getBlockBuilder().bitReverse(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final BlockLiteral node) {
                return param.ctxt.getLiteralFactory().literalOf(param.copyBlock(BlockLabel.getTargetOf(node.getBlockLabel())));
            }

            public Value visit(final Copier param, final BooleanLiteral node) {
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

            public Value visit(final Copier param, final Clone node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().clone(param.copyValue(node.getInput()));
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
                    param.copyValue(node.getUpdateValue()), node.getSuccessAtomicityMode(), node.getFailureAtomicityMode(), node.getStrength());
            }

            public Value visit(Copier param, CmpG node) {
                return param.getBlockBuilder().cmpG(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(Copier param, CmpL node) {
                return param.getBlockBuilder().cmpL(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
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

            public Value visit(final Copier param, final Convert node) {
                return param.getBlockBuilder().valueConvert(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final CurrentThreadRead node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().currentThread();
            }

            public Value visit(final Copier param, final Div node) {
                return param.getBlockBuilder().divide(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public ValueHandle visit(Copier param, ElementOf node) {
                return param.getBlockBuilder().elementOf(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getIndex()));
            }

            public ValueHandle visit(Copier param, ExactMethodElementHandle node) {
                return param.getBlockBuilder().exactMethodOf(param.copyValue(node.getInstance()), node.getExecutable(), node.getCallSiteDescriptor(), node.getCallSiteType());
            }

            public ValueHandle visit(Copier param, FunctionDeclarationHandle node) {
                return param.getBlockBuilder().functionOf(node.getProgramObject());
            }

            public ValueHandle visit(Copier param, FunctionElementHandle node) {
                return param.getBlockBuilder().functionOf(node.getExecutable());
            }

            public ValueHandle visit(Copier param, FunctionHandle node) {
                return param.getBlockBuilder().functionOf(node.getProgramObject());
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

            public Value visit(final Copier param, final GetAndAdd node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().getAndAdd(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getUpdateValue()), node.getAtomicityMode());
            }

            public Value visit(final Copier param, final GetAndBitwiseAnd node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().getAndBitwiseAnd(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getUpdateValue()), node.getAtomicityMode());
            }

            public Value visit(final Copier param, final GetAndBitwiseNand node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().getAndBitwiseNand(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getUpdateValue()), node.getAtomicityMode());
            }

            public Value visit(final Copier param, final GetAndBitwiseOr node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().getAndBitwiseOr(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getUpdateValue()), node.getAtomicityMode());
            }

            public Value visit(final Copier param, final GetAndBitwiseXor node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().getAndBitwiseXor(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getUpdateValue()), node.getAtomicityMode());
            }

            public Value visit(final Copier param, final GetAndSet node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().getAndSet(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getUpdateValue()), node.getAtomicityMode());
            }

            public Value visit(final Copier param, final GetAndSetMax node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().getAndSetMax(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getUpdateValue()), node.getAtomicityMode());
            }

            public Value visit(final Copier param, final GetAndSetMin node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().getAndSetMin(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getUpdateValue()), node.getAtomicityMode());
            }

            public Value visit(final Copier param, final GetAndSub node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().getAndSub(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getUpdateValue()), node.getAtomicityMode());
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
                return param.getBlockBuilder().load(param.copyValueHandle(node.getValueHandle()), node.getMode());
            }

            public ValueHandle visit(Copier param, LocalVariable node) {
                return param.getBlockBuilder().localVariable(node.getVariableElement());
            }

            public ValueHandle visit(Copier param, MemberOf node) {
                return param.getBlockBuilder().memberOf(param.copyValueHandle(node.getValueHandle()), node.getMember());
            }

            public Value visit(final Copier param, final MethodHandleLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final Max node) {
                return param.getBlockBuilder().max(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
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
                return param.getBlockBuilder().new_(node.getClassObjectType());
            }

            public Value visit(final Copier param, final NewArray node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().newArray(node.getArrayType(), param.copyValue(node.getSize()));
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

            public Value visit(final Copier param, final ParameterValue node) {
                return node;
            }

            static final PhiValue.Flag[] NO_FLAGS = new PhiValue.Flag[0];
            static final PhiValue.Flag[] NOT_NULL_FLAGS = new PhiValue.Flag[] { PhiValue.Flag.NOT_NULL };

            public Value visit(final Copier param, final PhiValue node) {
                param.enqueue(node);
                return param.getBlockBuilder().phi(node.getType(), param.copyBlock(node.getPinnedBlock()), node.possibleValuesAreNullable() ? NO_FLAGS : NOT_NULL_FLAGS);
            }

            public Value visit(final Copier param, final PopCount node) {
                return param.getBlockBuilder().populationCount(param.copyValue(node.getInput()));
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
                return param.getBlockBuilder().pointerHandle(param.copyValue(node.getPointerValue()));
            }

            public ValueHandle visit(Copier param, ReferenceHandle node) {
                return param.getBlockBuilder().referenceHandle(param.copyValue(node.getReferenceValue()));
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
                return param.getBlockBuilder().store(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getValue()), node.getMode());
            }

            public Value visit(final Copier param, final StringLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final Sub node) {
                return param.getBlockBuilder().sub(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final SymbolLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final Truncate node) {
                return param.getBlockBuilder().truncate(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final TypeIdOf node) {
                return param.getBlockBuilder().typeIdOf(param.copyValueHandle(node.getValueHandle()));
            }

            public Value visit(final Copier param, final TypeLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final UndefinedLiteral node) {
                return node;
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

            public Value visit(final Copier param, final Deref node) {
                return param.getBlockBuilder().deref(param.copyValue(node.getInput()));
            }
        }
    }
}
