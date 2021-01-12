package cc.quarkus.qcc.graph;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiFunction;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.literal.ArrayLiteral;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.BooleanLiteral;
import cc.quarkus.qcc.graph.literal.CompoundLiteral;
import cc.quarkus.qcc.graph.literal.CurrentThreadLiteral;
import cc.quarkus.qcc.graph.literal.DefinedConstantLiteral;
import cc.quarkus.qcc.graph.literal.FloatLiteral;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.MethodDescriptorLiteral;
import cc.quarkus.qcc.graph.literal.MethodHandleLiteral;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.graph.literal.ObjectLiteral;
import cc.quarkus.qcc.graph.literal.StringLiteral;
import cc.quarkus.qcc.graph.literal.SymbolLiteral;
import cc.quarkus.qcc.graph.literal.TypeLiteral;
import cc.quarkus.qcc.graph.literal.UndefinedLiteral;
import cc.quarkus.qcc.graph.literal.ZeroInitializerLiteral;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public interface Node {
    int getSourceLine();

    int getBytecodeIndex();

    default int getValueDependencyCount() {
        return 0;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    default int getBasicDependencyCount() {
        return 0;
    }

    default Node getBasicDependency(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    default Node getDependency() {
        int cnt = getBasicDependencyCount();
        if (cnt == 1) {
            return getBasicDependency(0);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * A node copier, which uses a visitor chain to allow observation and transformation of the graph nodes as they
     * are copied.
     */
    final class Copier {
        private final BasicBlock entryBlock;
        private final BasicBlockBuilder blockBuilder;
        private final NodeVisitor<Copier, Value, Node, BasicBlock> nodeVisitor;
        private final Map<BasicBlock, BlockLabel> copiedBlocks = new HashMap<>();
        private final HashMap<Node, Node> copiedNodes = new HashMap<>();
        private final Queue<PhiValue> phiQueue = new ArrayDeque<>();
        private final Queue<BasicBlock> blockQueue = new ArrayDeque<>();
        private final Terminus terminus = new Terminus();
        private final CompilationContext ctxt;

        Copier(BasicBlock entryBlock, BasicBlockBuilder builder, CompilationContext ctxt,
            BiFunction<CompilationContext, NodeVisitor<Copier, Value, Node, BasicBlock>, NodeVisitor<Copier, Value, Node, BasicBlock>> nodeVisitorFactory
        ) {
            this.entryBlock = entryBlock;
            this.ctxt = ctxt;
            blockBuilder = builder;
            nodeVisitor = nodeVisitorFactory.apply(ctxt, terminus);
        }

        public static BasicBlock execute(BasicBlock entryBlock, BasicBlockBuilder builder, CompilationContext param,
            BiFunction<CompilationContext, NodeVisitor<Copier, Value, Node, BasicBlock>, NodeVisitor<Copier, Value, Node, BasicBlock>> nodeVisitorFactory
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
                for (final BasicBlock incomingBlock : orig.incomingBlocks()) {
                    if (incomingBlock.isReachable()) {
                        Value val = orig.getValueForBlock(incomingBlock);
                        if (val != null) {
                            copy.setValueForBlock(ctxt, blockBuilder.getCurrentElement(), copiedBlocks.get(incomingBlock), copyValue(val));
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

        public void copyNode(Node original) {
            if (original instanceof Value) {
                copyValue((Value) original);
            } else if (original instanceof Action) {
                copyAction((Action) original);
            } else {
                assert original instanceof Terminator;
                copyTerminator((Terminator) original);
            }
        }

        public Value copyValue(Value original) {
            Value copy = (Value) copiedNodes.get(original);
            if (copy == null) {
                int oldLine = blockBuilder.setLineNumber(original.getSourceLine());
                int oldBci = blockBuilder.setBytecodeIndex(original.getBytecodeIndex());
                try {
                    copy = original.accept(nodeVisitor, this);
                    copiedNodes.put(original, copy);
                } finally {
                    blockBuilder.setLineNumber(oldLine);
                    blockBuilder.setBytecodeIndex(oldBci);
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
                try {
                    copy = original.accept(nodeVisitor, this);
                    copiedNodes.put(original, copy);
                } finally {
                    blockBuilder.setLineNumber(oldLine);
                    blockBuilder.setBytecodeIndex(oldBci);
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
            // terminators can only be visited one time, by definition
            int oldLine = blockBuilder.setLineNumber(original.getSourceLine());
            int oldBci = blockBuilder.setBytecodeIndex(original.getBytecodeIndex());
            try {
                return original.accept(nodeVisitor, this);
            } finally {
                blockBuilder.setLineNumber(oldLine);
                blockBuilder.setBytecodeIndex(oldBci);
            }
        }

        public PhiValue enqueue(PhiValue originalPhi) {
            phiQueue.add(Assert.checkNotNullParam("originalPhi", originalPhi));
            return originalPhi;
        }

        static class Terminus implements NodeVisitor<Copier, Value, Node, BasicBlock> {
            public Node visitUnknown(final Copier param, final Action node) {
                throw Assert.unreachableCode();
            }

            public BasicBlock visitUnknown(final Copier param, final Terminator node) {
                throw Assert.unreachableCode();
            }

            public Value visitUnknown(final Copier param, final Value node) {
                throw Assert.unreachableCode();
            }

            public Node visit(Copier param, ArrayElementWrite node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().writeArrayValue(param.copyValue(node.getInstance()), param.copyValue(node.getIndex()), param.copyValue(node.getWriteValue()), node.getMode());
            }

            public Node visit(Copier param, BlockEntry node) {
                return param.getBlockBuilder().getBlockEntry();
            }

            public Node visit(Copier param, InstanceFieldWrite node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().writeInstanceField(param.copyValue(node.getInstance()), node.getFieldElement(), param.copyValue(node.getWriteValue()), node.getMode());
            }

            public Node visit(Copier param, DynamicInvocation node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().invokeDynamic(node.getBootstrapMethod(), param.copyValues(node.getStaticArguments()), param.copyValues(node.getArguments()));
            }

            public Node visit(Copier param, InstanceInvocation node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().invokeInstance(node.getKind(), param.copyValue(node.getInstance()), node.getInvocationTarget(), param.copyValues(node.getArguments()));
            }

            public Node visit(Copier param, MonitorEnter node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().monitorEnter(param.copyValue(node.getInstance()));
            }

            public Node visit(Copier param, MonitorExit node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().monitorExit(param.copyValue(node.getInstance()));
            }

            public Node visit(Copier param, PointerStore node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().pointerStore(param.copyValue(node.getPointer()), param.copyValue(node.getValue()), node.getAccessMode(), node.getAtomicityMode());
            }

            public Node visit(Copier param, StaticFieldWrite node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().writeStaticField(node.getFieldElement(), param.copyValue(node.getWriteValue()), node.getMode());
            }

            public Node visit(Copier param, StaticInvocation node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().invokeStatic(node.getInvocationTarget(), param.copyValues(node.getArguments()));
            }

            public BasicBlock visit(Copier param, Goto node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().goto_(param.copyBlock(node.getResumeTarget()));
            }

            public BasicBlock visit(Copier param, If node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().if_(param.copyValue(node.getCondition()), param.copyBlock(node.getTrueBranch()), param.copyBlock(node.getFalseBranch()));
            }

            public BasicBlock visit(Copier param, Jsr node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().jsr(param.copyBlock(node.getResumeTarget()), (BlockLiteral) param.copyValue(node.getReturnAddressValue()));
            }

            public BasicBlock visit(Copier param, Ret node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().ret(param.copyValue(node.getReturnAddressValue()));
            }

            public BasicBlock visit(Copier param, Return node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().return_();
            }

            public BasicBlock visit(Copier param, Switch node) {
                param.copyNode(node.getBasicDependency(0));
                int cnt = node.getNumberOfValues();
                BlockLabel[] targetsCopy = new BlockLabel[cnt];
                for (int i = 0; i < cnt; i ++) {
                    targetsCopy[i] = param.copyBlock(node.getTargetForIndex(i));
                }
                return param.getBlockBuilder().switch_(param.copyValue(node.getSwitchValue()), node.getValues(), targetsCopy, param.copyBlock(node.getDefaultTarget()));
            }

            public BasicBlock visit(Copier param, Throw node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().throw_(param.copyValue(node.getThrownValue()));
            }

            public BasicBlock visit(Copier param, Try node) {
                param.copyNode(node.getBasicDependency(0));
                Node copied = param.copyTriable(node.getDelegateOperation());
                BlockLabel resumeLabel = param.copyBlock(node.getResumeTarget());
                if (copied instanceof Triable) {
                    return param.getBlockBuilder().try_((Triable) copied, resumeLabel, param.copyBlock(BlockLabel.getTargetOf(node.getExceptionHandlerLabel())));
                } else {
                    return param.getBlockBuilder().goto_(resumeLabel);
                }
            }

            public BasicBlock visit(Copier param, ValueReturn node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().return_(param.copyValue(node.getReturnValue()));
            }

            public BasicBlock visit(Copier param, ClassCastErrorNode node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().classCastException(param.copyValue(node.getFromType()), param.copyValue(node.getToType()));
            }

            public BasicBlock visit(Copier param, NoSuchMethodErrorNode node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().noSuchMethodError(node.getOwner(), node.getDescriptor(), node.getName());
            }

            public BasicBlock visit(Copier param, ClassNotFoundErrorNode node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().classNotFoundError(node.getName());
            }

            public Value visit(final Copier param, final Add node) {
                return param.getBlockBuilder().add(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final And node) {
                return param.getBlockBuilder().and(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final ArrayElementRead node) {
                return param.getBlockBuilder().readArrayValue(param.copyValue(node.getInstance()), param.copyValue(node.getIndex()), node.getMode());
            }

            public Value visit(final Copier param, final ArrayLength node) {
                return param.getBlockBuilder().arrayLength(param.copyValue(node.getInstance()));
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

            public Value visit(final Copier param, final ClassOf node) {
                return param.getBlockBuilder().classOf(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final Clone node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().clone(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final CmpEq node) {
                return param.getBlockBuilder().cmpEq(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final CmpGe node) {
                return param.getBlockBuilder().cmpGe(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final CmpGt node) {
                return param.getBlockBuilder().cmpGt(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final CmpLe node) {
                return param.getBlockBuilder().cmpLe(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final CmpLt node) {
                return param.getBlockBuilder().cmpLt(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final CmpNe node) {
                return param.getBlockBuilder().cmpNe(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final CompoundLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final ConstructorInvocation node) {
                return param.getBlockBuilder().invokeConstructor(param.copyValue(node.getInstance()), node.getInvocationTarget(), param.copyValues(node.getArguments()));
            }

            public Value visit(final Copier param, final Convert node) {
                return param.getBlockBuilder().valueConvert(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final CurrentThreadLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final DefinedConstantLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final Div node) {
                return param.getBlockBuilder().divide(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final DynamicInvocationValue node) {
                return param.getBlockBuilder().invokeValueDynamic(node.getBootstrapMethod(), param.copyValues(node.getStaticArguments()), node.getType(), param.copyValues(node.getArguments()));
            }

            public Value visit(final Copier param, final Extend node) {
                return param.getBlockBuilder().extend(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final FloatLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final FunctionCall node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().callFunction(param.copyValue(node.getCallTarget()), param.copyValues(node.getArguments()));
            }

            public Value visit(final Copier param, final InstanceFieldRead node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().readInstanceField(param.copyValue(node.getInstance()), node.getFieldElement(), node.getMode());
            }

            public Value visit(final Copier param, final InstanceInvocationValue node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().invokeValueInstance(node.getKind(), param.copyValue(node.getInstance()), node.getInvocationTarget(), param.copyValues(node.getArguments()));
            }

            public Value visit(final Copier param, final InstanceOf node) {
                return param.getBlockBuilder().instanceOf(param.copyValue(node.getInstance()), node.getCheckType());
            }

            public Value visit(final Copier param, final IntegerLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final MemberPointer node) {
                return param.getBlockBuilder().memberPointer(param.copyValue(node.getStructPointer()), node.getMember());
            }

            public Value visit(final Copier param, final MethodDescriptorLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final MethodHandleLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final Mod node) {
                return param.getBlockBuilder().remainder(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final MultiNewArray node) {
                return param.getBlockBuilder().multiNewArray(node.getArrayType(), param.copyValues(node.getDimensions()));
            }

            public Value visit(final Copier param, final Multiply node) {
                return param.getBlockBuilder().multiply(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final Narrow node) {
                return param.getBlockBuilder().narrow(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final Neg node) {
                return param.getBlockBuilder().negate(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final New node) {
                return node;
            }

            public Value visit(final Copier param, final NewArray node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().newArray(node.getArrayType(), param.copyValue(node.getSize()));
            }

            public Value visit(final Copier param, final NullLiteral node) {
                return node;
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
                for (Map.Entry<BasicBlock, Value> entry : node.getIncomingValues()) {
                    // ensure all reachable incoming blocks are enqueued
                    BasicBlock block = entry.getKey();
                    if (block.isReachable()) {
                        param.copyBlock(block);
                    }
                }
                return param.getBlockBuilder().phi(node.getType(), param.copyBlock(node.getPinnedBlock()));
            }

            public Value visit(final Copier param, final PointerLoad node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().pointerLoad(param.copyValue(node.getPointer()), node.getAccessMode(), node.getAtomicityMode());
            }

            public Value visit(final Copier param, final Rol node) {
                return param.getBlockBuilder().rol(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final Ror node) {
                return param.getBlockBuilder().ror(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
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
                return param.getBlockBuilder().stackAllocate(node.getType(), param.copyValue(node.getCount()), param.copyValue(node.getAlign()));
            }

            public Value visit(final Copier param, final StaticFieldRead node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().readStaticField(node.getFieldElement(), node.getMode());
            }

            public Value visit(final Copier param, final StaticInvocationValue node) {
                param.copyNode(node.getBasicDependency(0));
                return param.getBlockBuilder().invokeValueStatic(node.getInvocationTarget(), param.copyValues(node.getArguments()));
            }

            public Value visit(final Copier param, final StringLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final SymbolLiteral node) {
                return node;
            }

            public Value visit(final Copier param, final Sub node) {
                return param.getBlockBuilder().sub(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final ThisValue node) {
                return node;
            }

            public Value visit(final Copier param, final Truncate node) {
                return param.getBlockBuilder().truncate(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final TypeIdOf node) {
                return param.getBlockBuilder().typeIdOf(param.copyValue(node.getInstance()));
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
