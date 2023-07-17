package org.qbicc.graph;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;

import io.smallrye.common.constraint.Assert;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.literal.ArrayLiteral;
import org.qbicc.graph.literal.BitCastLiteral;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.graph.literal.EncodeReferenceLiteral;
import org.qbicc.graph.literal.StructLiteral;
import org.qbicc.graph.literal.ElementOfLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.MemberOfLiteral;
import org.qbicc.graph.literal.OffsetFromLiteral;
import org.qbicc.graph.schedule.Util;
import org.qbicc.type.StructType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public interface Node extends ProgramLocatable {
    /**
     * Get the call site node if this node is part of an inlined function.  The call site node may be of
     * any type, but must refer to the source line, bytecode index, and enclosing element of the call site
     * that was inlined.
     *
     * @return the call site node, or {@code null} if this node was not inlined from another function
     */
    Node callSite();

    /**
     * Get the source element.  Literals will have no source element.
     *
     * @return the source element, or {@code null} if there is none
     */
    ExecutableElement element();

    int lineNumber();

    int bytecodeIndex();

    int getScheduleIndex();

    void setScheduleIndex(int index);

    BasicBlock getScheduledBlock();

    void setScheduledBlock(BasicBlock block);

    Set<Value> getLiveIns();

    void setLiveIns(Set<Value> live);

    Set<Value> getLiveOuts();

    void setLiveOuts(Set<Value> live);

    default int getValueDependencyCount() {
        return 0;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    StringBuilder toString(StringBuilder b);

    default int getBlockIndex() {
        return getScheduledBlock().getIndex();
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
        private final HashMap<Terminator, BasicBlock> copiedTerminators = new HashMap<>();
        private final Queue<BasicBlock> blockQueue = new ArrayDeque<>();
        private final Terminus terminus = new Terminus();
        private final CompilationContext ctxt;
        private final Map<Set<Value>, Set<Value>> cache = new HashMap<>();
        private Set<Value> liveOut;
        private Set<Value> liveIn;

        public Copier(BasicBlock entryBlock, BasicBlockBuilder builder, CompilationContext ctxt,
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

        public void copyBlockAs(BasicBlock original, BlockLabel copy) {
            if (copiedBlocks.putIfAbsent(original, copy) != null) {
                throw new IllegalStateException();
            }
        }

        public void copyScheduledNodes(BasicBlock block) {
            try {
                final List<Node> instructions = block.getInstructions();
                mapInstructions(block, instructions.listIterator(instructions.size()), new HashSet<>(block.getLiveOuts()), block.getLiveOuts());
            } catch (BlockEarlyTermination term) {
                copiedTerminators.put(block.getTerminator(), term.getTerminatedBlock());
            }
        }

        private void mapInstructions(BasicBlock block, ListIterator<Node> iter, Set<Value> live, Set<Value> liveOuts) {
            if (! iter.hasPrevious()) {
                // all done
                return;
            }
            // process current instruction
            final Node node = iter.previous();
            if (node instanceof BlockParameter bp && bp.getPinnedBlock() == block) {
                // keep it alive to the start of the block.
            } if (node instanceof Value v) {
                // this is where it was defined, thus we can remove it from the live set.
                live.remove(v);
            } else if (node instanceof Invoke inv) {
                // special case!
                live.remove(inv.getReturnValue());
            }
            // add any values consumed by this node to the live set
            final int cnt = node.getValueDependencyCount();
            for (int i = 0; i < cnt; i ++) {
                final Value val = node.getValueDependency(i);
                if (val.getType() instanceof ReferenceType && ! (val instanceof Literal)) {
                    live.add(val);
                }
            }
            Set<Value> liveIn = Util.getCachedSet(cache, live);
            // process and emit previous instruction (if any)
            mapInstructions(block, iter, live, liveIn);
            // now emit current instruction
            this.liveIn = liveIn;
            this.liveOut = liveOuts;
            copyNode(node);
        }

        /**
         * Get the set of values which were live in the program before this node was reached.
         *
         * @return the set of live values
         */
        public Set<Value> getLiveIn() {
            return liveIn;
        }

        /**
         * Get the set of values which will be live in the program after this node is processed.
         * This set might or might not include the node currently being processed.
         *
         * @return the set of live values
         */
        public Set<Value> getLiveOut() {
            return liveOut;
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
                if (! (original instanceof Unschedulable) && original.getScheduledBlock() == null) {
                    blockBuilder.getContext().warning(blockBuilder.element(), "Converting unscheduled node %s to unreachable()", original.toString());
                    throw new BlockEarlyTermination(blockBuilder.unreachable());
                }
                if (original instanceof Literal lit) {
                    // use line, bci etc. of previous item
                    copy = lit.accept(nodeVisitor, this);
                    copiedNodes.put(original, copy);
                    return copy;
                }
                int oldLine = blockBuilder.setLineNumber(original.lineNumber());
                int oldBci = blockBuilder.setBytecodeIndex(original.bytecodeIndex());
                ExecutableElement oldElement = blockBuilder.setCurrentElement(original.element());
                Node origCallSite = original.callSite();
                Node oldCallSite = origCallSite == null ? blockBuilder.callSite() : blockBuilder.setCallSite(copyNode(origCallSite));
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

        public List<Literal> copyLiterals(List<Literal> list) {
            if (list.isEmpty()) {
                return List.of();
            }
            Literal[] values = new Literal[list.size()];
            int i = 0;
            for (Literal original : list) {
                values[i++] = (Literal)copyValue(original);
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
                if (! (original instanceof Unschedulable) && original.getScheduledBlock() == null) {
                    throw new IllegalStateException("Missing schedule for node");
                }
                int oldLine = blockBuilder.setLineNumber(original.lineNumber());
                int oldBci = blockBuilder.setBytecodeIndex(original.bytecodeIndex());
                ExecutableElement oldElement = blockBuilder.setCurrentElement(original.element());
                Node origCallSite = original.callSite();
                Node oldCallSite = origCallSite == null ? blockBuilder.callSite() : blockBuilder.setCallSite(copyNode(origCallSite));
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
                if (! (original instanceof Unschedulable) && original.getScheduledBlock() == null) {
                    throw new IllegalStateException("Missing schedule for node");
                }
                // copy the terminator and its dependencies
                int oldLine = blockBuilder.setLineNumber(original.lineNumber());
                int oldBci = blockBuilder.setBytecodeIndex(original.bytecodeIndex());
                ExecutableElement oldElement = blockBuilder.setCurrentElement(original.element());
                Node origCallSite = original.callSite();
                Node oldCallSite = origCallSite == null ? blockBuilder.callSite() : blockBuilder.setCallSite(copyNode(origCallSite));
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

            public Value visitAny(Copier copier, Literal literal) {
                // literals without value dependencies remain unchanged by default
                Assert.assertTrue(literal.getValueDependencyCount() == 0);
                return literal;
            }

            public Value visit(Copier copier, ArrayLiteral literal) {
                List<Literal> values = copier.copyLiterals(literal.getValues());
                return copier.getBlockBuilder().getLiteralFactory().literalOf(literal.getType(), values);
            }

            public Value visit(Copier copier, BitCastLiteral literal) {
                Literal value = (Literal)copier.copyValue(literal.getValue());
                if (value == literal.getValue()) {
                    return literal;
                } else {
                    return copier.getBlockBuilder().getLiteralFactory().bitcastLiteral(value, literal.getType());
                }
            }

            public Value visit(Copier copier, StructLiteral literal) {
                Map<StructType.Member, Literal> old = literal.getValues();
                Map<StructType.Member, Literal> copied = new HashMap<>();
                for (Map.Entry<StructType.Member, Literal> e : old.entrySet()) {
                    copied.put(e.getKey(), (Literal)copier.copyValue(e.getValue()));
                }
                return copier.getBlockBuilder().getLiteralFactory().literalOf(literal.getType(), copied);
            }

            public Value visit(Copier copier, ElementOfLiteral literal) {
                Literal arrayPointer = (Literal)copier.copyValue(literal.getArrayPointer());
                Literal index = (Literal)copier.copyValue(literal.getIndex());
                if (arrayPointer == literal.getArrayPointer() && index == literal.getIndex()) {
                    return literal;
                } else {
                    return copier.getBlockBuilder().getLiteralFactory().elementOfLiteral(arrayPointer, index);
                }
            }

            public Value visit(Copier copier, EncodeReferenceLiteral literal) {
                Literal value = (Literal)copier.copyValue(literal.getValue());
                if (value == literal.getValue()) {
                    return literal;
                } else {
                    return copier.getBlockBuilder().getLiteralFactory().encodeReferenceLiteral(value, literal.getType());
                }
            }

            public Value visit(Copier copier, OffsetFromLiteral literal) {
                Literal basePointer = (Literal)copier.copyValue(literal.getBasePointer());
                Literal offset = (Literal)copier.copyValue(literal.getOffset());
                if (basePointer == literal.getBasePointer() && offset == literal.getOffset()) {
                    return literal;
                } else {
                    return copier.getBlockBuilder().getLiteralFactory().offsetFromLiteral(basePointer, offset);
                }
            }

            @Override
            public Value visit(Copier copier, MemberOfLiteral literal) {
                Literal structPointer = (Literal)copier.copyValue(literal.getStructurePointer());
                if (structPointer == literal.getStructurePointer()) {
                    return literal;
                } else {
                    return copier.getBlockBuilder().getLiteralFactory().memberOfLiteral(structPointer, literal.getMember());
                }
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

            public Node visit(Copier copier, InitializeClass node) {
                copier.copyNode(node.getDependency());
                return copier.getBlockBuilder().initializeClass(copier.copyValue(node.getInitializeClassValue()));
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
                return param.getBlockBuilder().callNoReturn(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()));
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
                    invoke = param.getBlockBuilder().invoke(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()), param.copyBlock(node.getCatchBlock()), param.copyBlock(node.getResumeTarget()), param.copyArguments(node));
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
                return param.getBlockBuilder().invokeNoReturn(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()), param.copyBlock(node.getCatchBlock()), param.copyArguments(node));
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
                return param.getBlockBuilder().tailCall(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()));
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

            public Value visit(final Copier param, final And node) {
                return param.getBlockBuilder().and(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier copier, final Auto node) {
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

            public Value visit(final Copier copier, final BlockParameter node) {
                return copier.getBlockBuilder().addParam(copier.copyBlock(node.getPinnedBlock()), node.getSlot(), node.getType(), node.possibleValuesAreNullable());
            }

            public Value visit(final Copier param, final ByteSwap node) {
                return param.getBlockBuilder().byteSwap(param.copyValue(node.getInput()));
            }

            public Value visit(final Copier param, final Call node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().call(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()));
            }

            public Value visit(final Copier param, final CallNoSideEffects node) {
                return param.getBlockBuilder().callNoSideEffects(param.copyValue(node.getTarget()), param.copyValue(node.getReceiver()), param.copyValues(node.getArguments()));
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
                return param.getBlockBuilder().cmpAndSwap(param.copyValue(node.getPointer()), param.copyValue(node.getExpectedValue()),
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

            public Value visit(Copier param, CurrentThread node) {
                return param.getBlockBuilder().currentThread();
            }

            public Value visit(Copier copier, DecodeReference node) {
                return copier.getBlockBuilder().decodeReference(copier.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final Div node) {
                return param.getBlockBuilder().divide(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(Copier copier, ElementOf node) {
                return copier.getBlockBuilder().elementOf(copier.copyValue(node.getArrayPointer()), copier.copyValue(node.getIndex()));
            }

            public Value visit(Copier copier, EncodeReference node) {
                return copier.getBlockBuilder().encodeReference(copier.copyValue(node.getInput()), node.getType());
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
                return param.getBlockBuilder().extractMember(param.copyValue(node.getStructValue()), node.getMember());
            }

            public Value visit(final Copier copier, final FpToInt node) {
                return copier.getBlockBuilder().fpToInt(copier.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final InsertElement node) {
                return param.getBlockBuilder().insertElement(param.copyValue(node.getArrayValue()), param.copyValue(node.getIndex()), param.copyValue(node.getInsertedValue()));
            }

            public Value visit(final Copier param, final InsertMember node) {
                return param.getBlockBuilder().insertMember(param.copyValue(node.getStructValue()), node.getMember(), param.copyValue(node.getInsertedValue()));
            }

            public Value visit(final Copier param, final InstanceOf node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().instanceOf(param.copyValue(node.getInstance()), node.getCheckType(), node.getCheckDimensions());
            }

            public Value visit(final Copier copier, final InterfaceMethodLookup node) {
                copier.copyNode(node.getDependency());
                return copier.getBlockBuilder().lookupInterfaceMethod(copier.copyValue(node.getReference()), node.getMethod());
            }

            public Value visit(final Copier copier, final IntToFp node) {
                return copier.getBlockBuilder().intToFp(copier.copyValue(node.getInput()), node.getType());
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

            public Value visit(final Copier param, final InstanceFieldOf node) {
                return param.getBlockBuilder().instanceFieldOf(param.copyValue(node.getInstance()), node.getVariableElement());
            }

            public Value visit(final Copier param, final Load node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().load(param.copyValue(node.getPointer()), node.getAccessMode());
            }

            public Value visit(Copier param, MemberOf node) {
                return param.getBlockBuilder().memberOf(param.copyValue(node.getStructurePointer()), node.getMember());
            }

            public Value visit(final Copier copier, final MemberOfUnion node) {
                return copier.getBlockBuilder().memberOfUnion(copier.copyValue(node.getUnionPointer()), node.getMember());
            }

            public Value visit(final Copier param, final Max node) {
                return param.getBlockBuilder().max(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final Dereference node) {
                return param.getBlockBuilder().deref(param.copyValue(node.getPointer()));
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

            public Value visit(final Copier param, final OffsetOfField node) {
                return param.getBlockBuilder().offsetOfField(node.getFieldElement());
            }

            public Value visit(final Copier copier, final OffsetPointer node) {
                return copier.getBlockBuilder().offsetPointer(copier.copyValue(node.getBasePointer()), copier.copyValue(node.getOffset()));
            }

            public Value visit(final Copier param, final Or node) {
                return param.getBlockBuilder().or(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier copier, final PointerDifference node) {
                return copier.getBlockBuilder().pointerDifference(copier.copyValue(node.getLeftInput()), copier.copyValue(node.getRightInput()));
            }

            public Value visit(final Copier param, final PopCount node) {
                return param.getBlockBuilder().populationCount(param.copyValue(node.getInput()));
            }

            public Value visit(Copier copier, ReadModifyWrite node) {
                return copier.getBlockBuilder().readModifyWrite(copier.copyValue(node.getPointer()), node.getOp(), copier.copyValue(node.getUpdateValue()), node.getReadAccessMode(), node.getWriteAccessMode());
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
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().stackAllocate(node.getType().getPointeeType(), param.copyValue(node.getCount()), param.copyValue(node.getAlign()));
            }

            public Node visit(final Copier param, final Store node) {
                param.copyNode(node.getDependency());
                return param.getBlockBuilder().store(param.copyValue(node.getPointer()), param.copyValue(node.getValue()), node.getAccessMode());
            }

            public Value visit(final Copier param, final Sub node) {
                return param.getBlockBuilder().sub(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }

            public Value visit(Copier copier, ThreadBound node) {
                return copier.getBlockBuilder().threadBound(copier.copyValue(node.getThreadPointer()), copier.copyValue(node.getTarget()));
            }

            public Value visit(final Copier param, final Truncate node) {
                return param.getBlockBuilder().truncate(param.copyValue(node.getInput()), node.getType());
            }

            public Value visit(final Copier param, final VaArg node) {
                return param.getBlockBuilder().vaArg(param.copyValue(node.getVaList()), node.getType());
            }

            public Value visit(Copier copier, VirtualMethodLookup node) {
                copier.copyNode(node.getDependency());
                return copier.getBlockBuilder().lookupVirtualMethod(copier.copyValue(node.getReference()), node.getMethod());
            }

            public Value visit(final Copier param, final ByteOffsetPointer node) {
                return param.getBlockBuilder().byteOffsetPointer(param.copyValue(node.getBasePointer()), param.copyValue(node.getOffset()), node.getOutputType());
            }

            public Value visit(final Copier param, final Xor node) {
                return param.getBlockBuilder().xor(param.copyValue(node.getLeftInput()), param.copyValue(node.getRightInput()));
            }
        }
    }
}
