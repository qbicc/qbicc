package cc.quarkus.qcc.graph;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.BiFunction;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.literal.ArrayLiteral;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.CompoundLiteral;
import cc.quarkus.qcc.graph.literal.DefinedConstantLiteral;
import cc.quarkus.qcc.graph.literal.FloatLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.MethodDescriptorLiteral;
import cc.quarkus.qcc.graph.literal.MethodHandleLiteral;
import cc.quarkus.qcc.graph.literal.ObjectLiteral;
import cc.quarkus.qcc.graph.literal.StringLiteral;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.TypeLiteral;
import cc.quarkus.qcc.graph.literal.UndefinedLiteral;
import cc.quarkus.qcc.graph.literal.ZeroInitializerLiteral;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
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

        Copier(BasicBlock entryBlock, BasicBlockBuilder builder, CompilationContext ctxt,
            BiFunction<CompilationContext, NodeVisitor<Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Copier, Value, Node, BasicBlock, ValueHandle>> nodeVisitorFactory
        ) {
            this.entryBlock = entryBlock;
            this.ctxt = ctxt;
            blockBuilder = builder;
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
                copyTerminator(block.getTerminator());
            }
            // now process all phis (all blocks will have been enqueued)
            PhiValue orig;
            while ((orig = phiQueue.poll()) != null) {
                PhiValue copy = (PhiValue) copiedNodes.get(orig);
                // process and map all incoming values - might enqueue more blocks or phis
                for (BasicBlock incomingBlock : orig.getPinnedBlock().getIncoming()) {
                    Terminator incomingTerminator = incomingBlock.getTerminator();
                    if (incomingBlock.isReachable()) {
                        Value val = orig.getValueForInput(incomingTerminator);
                        if (val != null) {
                            BasicBlock copiedIncomingBlock = copiedTerminators.get(incomingTerminator);
                            // if this block is null, that means that the copied block can no longer flow into this block due to transformation
                            if (copiedIncomingBlock != null) {
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

        public Node copyNode(Node original) {
            if (original instanceof Value) {
                return copyValue((Value) original);
            } else if (original instanceof Action) {
                return copyAction((Action) original);
            } else {
                assert original instanceof Terminator;
                BasicBlock block = copyTerminator((Terminator) original);
                return block.getTerminator();
            }
        }

        public Value copyValue(Value original) {
            Value copy = (Value) copiedNodes.get(original);
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

        List<Value> copyValues(List<Value> list) {
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

        public Node copyTriable(Triable original) {
            if (original instanceof Action) {
                return copyAction((Action) original);
            } else {
                assert original instanceof Value;
                return copyValue((Value) original);
            }
        }

        public BasicBlock copyTerminator(Terminator original) {
            BasicBlock basicBlock = copiedTerminators.get(original);
            if (basicBlock == null) {
                // first copy all outbound values from the original block
                Map<PhiValue, Value> outboundValues = ((AbstractTerminator) original).getOutboundValues();
                for (Value value : outboundValues.values()) {
                    copyValue(value);
                }
                // now copy the terminator and its dependencies
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

            public Node visit(Copier param, BlockEntry node) {
                return param.getBlockBuilder().getBlockEntry();
            }

            public Node visit(Copier param, InstanceInvocation node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().invokeInstance(node.getKind(), param.copyValue(node.getInstance()), node.getInvocationTarget(), param.copyValues(node.getArguments()));
            }

            public Node visit(Copier param, MonitorEnter node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().monitorEnter(param.copyValue(node.getInstance()));
            }

            public Node visit(Copier param, MonitorExit node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().monitorExit(param.copyValue(node.getInstance()));
            }

            public Node visit(Copier param, StaticInvocation node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().invokeStatic(node.getInvocationTarget(), param.copyValues(node.getArguments()));
            }

            public Node visit(Copier param, Fence node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().fence(node.getAtomicityMode());
            }

            public BasicBlock visit(Copier param, Goto node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().goto_(param.copyBlock(node.getResumeTarget()));
            }

            public BasicBlock visit(Copier param, If node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().if_(param.copyValue(node.getCondition()), param.copyBlock(node.getTrueBranch()), param.copyBlock(node.getFalseBranch()));
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

            public BasicBlock visit(Copier param, Unreachable node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().unreachable();
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

            public BasicBlock visit(Copier param, Throw node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().throw_(param.copyValue(node.getThrownValue()));
            }

            public BasicBlock visit(Copier param, Try node) {
                param.copyNode(node.getDependency());
                Node copied = param.copyTriable(node.getDelegateOperation());
                BlockLabel resumeLabel = param.copyBlock(node.getResumeTarget());
                if (copied instanceof Triable) {
                    return param.getBlockBuilder().try_((Triable) copied, resumeLabel, param.copyBlock(BlockLabel.getTargetOf(node.getExceptionHandlerLabel())));
                } else {
                    return param.getBlockBuilder().goto_(resumeLabel);
                }
            }

            public BasicBlock visit(Copier param, ValueReturn node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().return_(param.copyValue(node.getReturnValue()));
            }

            public BasicBlock visit(Copier param, ClassCastErrorNode node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().classCastException(param.copyValue(node.getFromType()), param.copyValue(node.getToType()));
            }

            public BasicBlock visit(Copier param, NoSuchMethodErrorNode node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().noSuchMethodError(node.getOwner(), node.getDescriptor(), node.getName());
            }

            public BasicBlock visit(Copier param, ClassNotFoundErrorNode node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().classNotFoundError(node.getName());
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

            public Value visit(final Copier param, final BlockLiteral node) {
                return param.ctxt.getLiteralFactory().literalOf(param.copyBlock(BlockLabel.getTargetOf(node.getBlockLabel())));
            }

            public Value visit(final Copier param, final BooleanLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final CheckCast node) {
                return param.getBlockBuilder().checkcast(param.copyValue(node.getInput()), param.copyValue(node.getToType()),
                    param.copyValue(node.getToDimensions()), node.getKind(), node.getType());
            }

            public Value visit(final Copier param, final ClassOf node) {
                return param.getBlockBuilder().classOf(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final Clone node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().clone(param.copyValue(node.getInput()));
            }

            public Value visit(Copier param, Cmp node) {
                return param.getBlockBuilder().cmp(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final CmpAndSwap node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().cmpAndSwap(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getExpectedValue()),
                    param.copyValue(node.getUpdateValue()), node.getSuccessAtomicityMode(), node.getFailureAtomicityMode());
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

            public Value visit(final Copier param, final ConstructorInvocation node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().invokeConstructor(param.copyValue(node.getInstance()), node.getInvocationTarget(), param.copyValues(node.getArguments()));
            }

            public Value visit(final Copier param, final Convert node) {
                return param.getBlockBuilder().valueConvert(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final CurrentThreadRead node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().currentThread();
            }

            public Value visit(final Copier param, final DefinedConstantLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final Div node) {
                return param.getBlockBuilder().divide(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public ValueHandle visit(Copier param, ElementOf node) {
                return param.getBlockBuilder().elementOf(param.copyValueHandle(node.getValueHandle()), param.copyValue(node.getIndex()));
            }

            public Value visit(final Copier param, final Extend node) {
                return param.getBlockBuilder().extend(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final FloatLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final FunctionCall node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().callFunction(param.copyValue(node.getCallTarget()), param.copyValues(node.getArguments()));
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

            public ValueHandle visit(Copier param, GlobalVariable node) {
                return param.getBlockBuilder().globalVariable(node.getVariableElement());
            }

            public Value visit(final Copier param, final InstanceInvocationValue node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().invokeValueInstance(node.getKind(), param.copyValue(node.getInstance()), node.getInvocationTarget(), param.copyValues(node.getArguments()));
            }

            public Value visit(final Copier param, final InstanceOf node) {
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

            public Value visit(final Copier param, final MethodDescriptorLiteral node) {
                return node;
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

            public Value visit(final Copier param, final ObjectLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final Or node) {
                return param.getBlockBuilder().or(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final ParameterValue node) {
                return node;
            }

            public Value visit(final Copier param, final PhiValue node) {
                param.enqueue(node);
                return param.getBlockBuilder().phi(node.getType(), param.copyBlock(node.getPinnedBlock()));
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
                return param.getBlockBuilder().stackAllocate(node.getType().getPointeeType(), param.copyValue(node.getCount()), param.copyValue(node.getAlign()));
            }

            public ValueHandle visit(Copier param, StaticField node) {
                return param.getBlockBuilder().staticField(node.getVariableElement());
            }

            public Value visit(final Copier param, final StaticInvocationValue node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().invokeValueStatic(node.getInvocationTarget(), param.copyValues(node.getArguments()));
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

            public Value visit(final Copier param, final Xor node) {
                return param.getBlockBuilder().xor(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final ZeroInitializerLiteral node) {
                return node;
            }
        }
    }
}
