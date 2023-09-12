package org.qbicc.machine.file.wasm.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.Ops;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.ValType;
import org.qbicc.machine.file.wasm.stream.InsnSeqVisitor;

/**
 * A sequence of instructions.
 * Once the sequence is ended, either by explicitly calling {@link #end()}
 * or by adding a {@link org.qbicc.machine.file.wasm.Ops#end end} instruction,
 * no more instructions may be added.
 * Instruction sequences are each distinct even if they contain identical instructions.
 */
// Cannot be record because it is mutable
public final class InsnSeq implements Iterable<Insn<?>> {
    private static final int DEFAULT_ESTIMATED_SIZE = 10;

    private final Flag.Set flags;
    private HashMap<Insn<?>, Insn<?>> cache;
    private final ArrayList<Insn<?>> instructions;
    private boolean foundElse = false;
    private boolean ended = false;

    InsnSeq(HashMap<Insn<?>, Insn<?>> cache, int estimatedSize, Flag.Set flags) {
        Assert.checkNotNullParam("flags", flags);
        this.cache = cache;
        this.flags = flags;
        instructions = new ArrayList<>(estimatedSize);
    }

    /**
     * Construct a new instance.
     *
     * @param estimatedSize the estimated number of instructions, not including {@code end}
     */
    public InsnSeq(int estimatedSize) {
        this(estimatedSize, Flag.Set.of());
    }

    public InsnSeq(int estimatedSize, Flag.Set flags) {
        this(null, estimatedSize, flags);
    }

    public InsnSeq(int estimatedSize, Flag flag0) {
        this(estimatedSize, Flag.Set.of(flag0));
    }

    public InsnSeq(int estimatedSize, Flag flag0, Flag flag1) {
        this(estimatedSize, Flag.Set.of(flag0, flag1));
    }

    /**
     * Construct a new instance.
     */
    public InsnSeq() {
        this(DEFAULT_ESTIMATED_SIZE, Flag.Set.of());
    }

    /**
     * Get the flags used by this instruction sequence.
     *
     * @return the flag set (not {@code null})
     */
    public Flag.Set flags() {
        return flags;
    }

    /**
     * Create a new empty instance, using the same instruction cache as this instance.
     *
     * @return the new, empty instruction sequence (not {@code null})
     */
    public InsnSeq newWithSharedCache() {
        return new InsnSeq(getCache(), DEFAULT_ESTIMATED_SIZE, Flag.Set.of());
    }

    /**
     * Create a new empty instance, using the same instruction cache as this instance.
     *
     * @return the new, empty instruction sequence (not {@code null})
     */
    public InsnSeq newWithSharedCache(Flag.Set flags) {
        return new InsnSeq(getCache(), DEFAULT_ESTIMATED_SIZE, flags);
    }

    /**
     * Pass all of the instructions of this sequence to the given visitor in order,
     * automatically adding {@code end} if it wasn't added to this sequence.
     *
     * @param <E> the exception type
     * @param ev the visitor (must not be {@code null})
     * @param encoder the encoder to use (must not be {@code null})
     * @throws E the exception thrown from the visitor
     */
    public <E extends Exception> void accept(final InsnSeqVisitor<E> ev, Insn.Encoder encoder) throws E {
        Assert.checkNotNullParam("ev", ev);
        for (Insn<?> instruction : instructions) {
            instruction.accept(ev, encoder);
        }
        // end is always implicit
        SimpleInsn.end.accept(ev, encoder);
    }

    public <O extends Op, I extends Insn<O>> I add(I insn) {
        Assert.checkNotNullParam("insn", insn);
        if (ended) {
            throw new IllegalArgumentException("Instruction added after `end`");
        }
        if (insn == SimpleInsn.end) {
            end();
        } else if (insn == SimpleInsn.else_) {
            if (flags.contains(Flag.ALLOW_ELSE) && ! foundElse) {
                instructions.add(insn);
                // there can be only one
                foundElse = true;
            } else {
                throw new IllegalArgumentException("Instruction `else` not allowed here");
            }
        } else {
            instructions.add(insn);
        }
        return insn;
    }

    public void end() {
        ended = true;
        instructions.trimToSize();
    }

    /**
     * Add a block instruction.
     * The given consumer should populate the sub-block; once it returns, the block will be ended.
     * To add a block which is not immediately terminated, use {@link #add(Insn)}.
     *
     * @param op the operation (must not be {@code null})
     * @param insnConsumer the instruction builder (must not be {@code null})
     * @return the instruction (not {@code null})
     */
    public BlockInsn add(Op.Block op, Consumer<BlockInsn> insnConsumer) {
        BlockInsn insn = new BlockInsn(op, new InsnSeq(cache, DEFAULT_ESTIMATED_SIZE, Flag.Set.of(op == Op.Block.if_, Flag.ALLOW_ELSE)));
        insnConsumer.accept(insn);
        insn.body().end();
        return add(insn);
    }

    /**
     * Add a label-indexed branch instruction.
     * The given branch target must enclose this instruction directly or indirectly.
     *
     * @param op the operation (must not be {@code null})
     * @param target the target block (must not be {@code null} and must enclose this instruction)
     * @return the newly created instruction (not {@code null})
     */
    public BranchInsn add(Op.Branch op, BranchTarget target) {
        return add(getCached(new BranchInsn(op, target)));
    }

    public ConstF32Insn add(Op.ConstF32 op, float val) {
        return add(getCached(new ConstF32Insn(op, val)));
    }

    public ConstF64Insn add(Op.ConstF64 op, double val) {
        return add(getCached(new ConstF64Insn(op, val)));
    }

    public ConstI32Insn add(Op.ConstI32 op, int val) {
        return add(getCached(new ConstI32Insn(op, val)));
    }

    public ConstI64Insn add(Op.ConstI64 op, long val) {
        return add(getCached(new ConstI64Insn(op, val)));
    }

    public ConstI128Insn add(Op.ConstI128 op, long low) {
        return add(getCached(new ConstI128Insn(op, low)));
    }

    public ConstI128Insn add(Op.ConstI128 op, long low, long high) {
        return add(getCached(new ConstI128Insn(op, low, high)));
    }

    public DataInsn add(Op.Data op, Segment segment) {
        return add(getCached(new DataInsn(op, segment)));
    }

    public DataInsn add(Op.Data op, SegmentHandle segmentHandle) {
        return add(getCached(new DataInsn(op, segmentHandle)));
    }

    public ElementInsn add(Op.Element op, Element element) {
        return add(getCached(new ElementInsn(op, element)));
    }

    public ElementInsn add(Op.Element op, ElementHandle elementHandle) {
        return add(getCached(new ElementInsn(op, elementHandle)));
    }

    public ElementAndTableInsn add(Op.ElementAndTable op, Element element, Table table) {
        return add(getCached(new ElementAndTableInsn(op, element, table)));
    }

    public ElementAndTableInsn add(Op.ElementAndTable op, ElementHandle elementHandle, Table table) {
        return add(getCached(new ElementAndTableInsn(op, elementHandle, table)));
    }

    public FuncInsn add(Op.Func op, Func func) {
        return add(getCached(new FuncInsn(op, func)));
    }

    public GlobalInsn add(Op.Global op, Global global) {
        return add(getCached(new GlobalInsn(op, global)));
    }

    public LaneInsn add(Op.Lane op, int laneIdx) {
        return add(getCached(new LaneInsn(op, laneIdx)));
    }

    public LocalInsn add(Op.Local op, int localIdx) {
        return add(getCached(new LocalInsn(op, localIdx)));
    }

    public MemoryInsn add(Op.Memory op, Memory memory) {
        return add(getCached(new MemoryInsn(op, memory)));
    }

    public MemoryAccessInsn add(Op.MemoryAccess op, Memory memory, int offset, int alignment) {
        return add(getCached(new MemoryAccessInsn(op, memory, offset, alignment)));
    }

    public MemoryAccessLaneInsn add(Op.MemoryAccessLane op, Memory memory, int offset, int alignment, int laneIdx) {
        return add(getCached(new MemoryAccessLaneInsn(op, memory, offset, alignment, laneIdx)));
    }

    public MemoryAndDataInsn add(Op.MemoryAndData op, Memory memory, Segment data) {
        return add(getCached(new MemoryAndDataInsn(op, memory, data)));
    }

    public MemoryAndDataInsn add(Op.MemoryAndData op, Memory memory, SegmentHandle segmentHandle) {
        return add(getCached(new MemoryAndDataInsn(op, memory, segmentHandle)));
    }

    public MemoryAndMemoryInsn add(Op.MemoryAndMemory op, Memory dest, Memory src) {
        return add(getCached(new MemoryAndMemoryInsn(op, dest, src)));
    }

    public MultiBranchInsn add(Op.MultiBranch op, List<BranchTarget> targets, BranchTarget defaultTarget) {
        return add(getCached(new MultiBranchInsn(op, targets, defaultTarget)));
    }

    public RefTypedInsn add(Op.RefTyped op, RefType type) {
        return add(RefTypedInsn.forOpAndType(op, type));
    }

    public SimpleInsn add(Op.Simple op) {
        return add(SimpleInsn.forOp(op));
    }

    public TableInsn add(Op.Table op, Table table) {
        return add(getCached(new TableInsn(op, table)));
    }

    public TableAndFuncTypeInsn add(Op.TableAndFuncType op, Table table, FuncType funcType) {
        return add(getCached(new TableAndFuncTypeInsn(op, table, funcType)));
    }

    public TableAndTableInsn add(Op.TableAndTable op, Table table1, Table table2) {
        return add(getCached(new TableAndTableInsn(op, table1, table2)));
    }

    public TypesInsn add(Op.Types op, ValType type) {
        return add(TypesInsn.forOpAndType(op, type));
    }

    public TypesInsn add(Op.Types op, ValType... types) {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("types", types);
        Assert.checkNotEmptyParam("types", types);
        if (types.length == 1) {
            return add(op, types[0]);
        } else {
            return add(op, List.of(types));
        }
    }

    public TypesInsn add(Op.Types op, List<ValType> types) {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("types", types);
        Assert.checkNotEmptyParam("types", types);
        if (types.size() == 1) {
            return add(op, types.get(0));
        } else {
            return add(new TypesInsn(op, types));
        }
    }

    @SuppressWarnings("unchecked")
    private <I extends Insn<?>> I getCached(final I instruction) {
        HashMap<Insn<?>, Insn<?>> cache = getCache();
        I cached = (I) cache.get(instruction);
        if (cached != null) {
            return cached;
        }
        cache.put(instruction, instruction);
        return instruction;
    }

    private HashMap<Insn<?>, Insn<?>> getCache() {
        HashMap<Insn<?>, Insn<?>> cache = this.cache;
        if (cache == null) {
            cache = this.cache = new HashMap<>();
        }
        return cache;
    }

    @Override
    public Iterator<Insn<?>> iterator() {
        Iterator<Insn<?>> iterator = instructions.iterator();
        return new Iterator<Insn<?>>() {
            boolean endDelivered;

            @Override
            public boolean hasNext() {
                return iterator.hasNext() || ended && !endDelivered;
            }

            @Override
            public Insn<?> next() {
                if (iterator.hasNext()) {
                    return iterator.next();
                } else if (ended && !endDelivered) {
                    endDelivered = true;
                    return SimpleInsn.end;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Insn<?>> action) {
        instructions.forEach(action);
        if (ended) {
            action.accept(SimpleInsn.end);
        }
    }

    public void forEachRecursive(Consumer<? super Insn<?>> action) {
        for (Insn<?> insn : this) {
            action.accept(insn);
            if (insn instanceof BlockInsn bi) {
                bi.body().forEachRecursive(action);
            }
        }
    }

    public <E extends Exception> InsnSeqVisitor<E> visitor(Insn.Resolver resolver) {
        return new PopulatingVisitor<E>(this, resolver);
    }

    /**
     * Get a visitor which can be used to populate an instruction sequence from a stream.
     * Nested blocks will be added as needed.
     * This class can be subclassed to add special behavior as needed.
     *
     * @param <E> the desired visitor exception type
     */
    public static class PopulatingVisitor<E extends Exception> extends InsnSeqVisitor<E> {
        private final ArrayList<BranchTarget> branchTargets;
        private final ArrayList<InsnSeq> stack;
        private final Insn.Resolver resolver;

        /**
         * Construct a new instance.
         *
         * @param seq the sequence to populate (must not be {@code null} and must not be ended)
         * @param resolver the resolver to use (must not be {@code null})
         */
        public PopulatingVisitor(final InsnSeq seq, final Insn.Resolver resolver) {
            Assert.checkNotNullParam("seq", seq);
            Assert.checkNotNullParam("resolver", resolver);
            this.resolver = resolver;
            branchTargets = new ArrayList<>();
            stack = new ArrayList<>();
            stack.add(seq);
        }

        /**
         * Construct a new instance for a function.
         *
         * @param seq the sequence to populate (must not be {@code null} and must not be ended)
         * @param resolver the resolver to use (must not be {@code null})
         * @param definedFunc the outermost branch target (must not be {@code null})
         */
        public PopulatingVisitor(final InsnSeq seq, final Insn.Resolver resolver, final DefinedFunc definedFunc) {
            this(seq, resolver);
            Assert.checkNotNullParam("definedFunc", definedFunc);
            branchTargets.add(definedFunc);
        }

        private InsnSeq current() {
            return stack.get(stack.size() - 1);
        }

        @Override
        public void visit(Op.Block insn) {
            InsnSeq child = current().newWithSharedCache(Flag.Set.of(insn == Op.Block.if_, Flag.ALLOW_ELSE));
            BlockInsn bi = new BlockInsn(insn, child);
            current().add(bi);
            branchTargets.add(bi);
            stack.add(child);
        }

        @Override
        public void visit(Op.Block insn, int typeIdx) {
            // todo...
            visit(insn);
        }

        @Override
        public void visit(Op.Block insn, ValType valType) {
            // todo...
            visit(insn);
        }

        @Override
        public void visit(Op.ConstF32 insn, float val) {
            current().add(insn, val);
        }

        @Override
        public void visit(Op.ConstF64 insn, double val) {
            current().add(insn, val);
        }

        @Override
        public void visit(Op.ConstI32 insn, int val) {
            current().add(insn, val);
        }

        @Override
        public void visit(Op.ConstI64 insn, long val) {
            current().add(insn, val);
        }

        @Override
        public void visit(Op.ConstI128 insn, long lowVal, long highVal) {
            current().add(insn, lowVal, highVal);
        }

        @Override
        public void visit(Op.Data insn, int dataIdx) {
            current().add(insn, resolver.resolveSegment(dataIdx));
        }

        @Override
        public void visit(Op.Element insn, int elemIdx) {
            current().add(insn, resolver.resolveElement(elemIdx));
        }

        @Override
        public void visit(Op.ElementAndTable insn, int elemIdx, int tableIdx) throws E {
            current().add(insn, resolver.resolveElement(elemIdx), resolver.resolveTable(tableIdx));
        }

        @Override
        public void visit(Op.Func insn, int funcIdx) {
            current().add(insn, resolver.resolveFunc(funcIdx));
        }

        @Override
        public void visit(Op.Global insn, int globalIdx) {
            current().add(insn, resolver.resolveGlobal(globalIdx));
        }

        @Override
        public void visit(Op.Local insn, int index) {
            current().add(insn, index);
        }

        @Override
        public void visit(Op.Branch insn, int index) {
            current().add(insn, branchTargets.get(index));
        }

        @Override
        public void visit(Op.Lane insn, int laneIdx) {
            current().add(insn, laneIdx);
        }

        @Override
        public void visit(Op.Memory insn, int memory) {
            current().add(insn, resolver.resolveMemory(memory));
        }

        @Override
        public void visit(Op.MemoryAccess insn, int memory, int align, int offset) {
            current().add(insn, resolver.resolveMemory(memory), offset, align);
        }

        @Override
        public void visit(Op.MemoryAccessLane insn, int memory, int align, int offset, int laneIdx) {
            current().add(insn, resolver.resolveMemory(memory), offset, align, laneIdx);
        }

        @Override
        public void visit(Op.MemoryAndData insn, int dataIdx, int memIdx) {
            current().add(insn, resolver.resolveMemory(memIdx), resolver.resolveSegment(dataIdx));
        }

        @Override
        public void visit(Op.MemoryAndMemory insn, int memIdx1, int memIdx2) {
            current().add(insn, resolver.resolveMemory(memIdx1), resolver.resolveMemory(memIdx2));
        }

        @Override
        public void visit(Op.Simple insn) {
            current().add(insn);
            if (insn == Ops.end) {
                stack.remove(stack.size() - 1);
                if (stack.isEmpty()) {
                    // done; no blocks left to pop
                    return;
                }
                branchTargets.remove(branchTargets.size() - 1);
            }
        }

        @Override
        public void visit(Op.MultiBranch insn, int defIndex, int... targetIndexes) {
            List<BranchTarget> mappedBlocks = new ArrayList<>(targetIndexes.length);
            for (int index : targetIndexes) {
                mappedBlocks.add(branchTargets.get(index));
            }
            current().add(insn, mappedBlocks, branchTargets.get(defIndex));
        }

        @Override
        public void visit(Op.Types insn, ValType... types) {
            current().add(insn, types);
        }

        @Override
        public void visit(Op.Table insn, int index) {
            current().add(insn, resolver.resolveTable(index));
        }

        @Override
        public void visit(Op.TableAndTable insn, int index1, int index2) {
            current().add(insn, resolver.resolveTable(index1), resolver.resolveTable(index2));
        }

        @Override
        public void visit(Op.TableAndFuncType insn, int tableIdx, int typeIdx) {
            current().add(insn, resolver.resolveTable(tableIdx), resolver.resolveFuncType(tableIdx));
        }

        @Override
        public void visit(Op.RefTyped insn, RefType type) {
            current().add(insn, type);
        }

        @Override
        public void visitEnd() throws E {
            while (! stack.isEmpty()) {
                stack.remove(stack.size() - 1).end();
            }
            branchTargets.clear();
        }
    }

    /**
     * The possible flags for configuring an instruction sequence.
     */
    public enum Flag {
        /**
         * Allow an {@link Ops#else_} instruction to occur in this block.
         */
        ALLOW_ELSE,
        ;

        static final Flag[] values = values();

        /**
         * A set of flags.
         * There are a finite number of possible flag sets, thus they are all preallocated.
         */
        public static final class Set {
            static final Set[] sets;

            static {
                int max = 1 << values.length;
                Set[] array = new Set[max];
                for (int i = 0; i < max; i ++) {
                    array[i] = new Set(i);
                }
                sets = array;
            }

            private final int bits;

            private Set(int bits) {
                this.bits = bits;
            }

            /**
             * Get the empty set.
             *
             * @return the empty set (not {@code null})
             */
            public static Set of() {
                return sets[0];
            }

            /**
             * Get the set comprising a single flag.
             *
             * @param flag the flag (must not be {@code null})
             * @return the set comprising {@code flag} (not {@code null})
             */
            public static Set of(Flag flag) {
                Assert.checkNotNullParam("flag", flag);
                return sets[1 << flag.ordinal()];
            }

            /**
             * Get the set comprising two flags.
             * If the flags are the same then the set will only contain that flag.
             *
             * @param flag0 the first flag (must not be {@code null})
             * @param flag1 the second flag (must not be {@code null})
             * @return the set comprising {@code flag0} and {@code flag1} (not {@code null})
             */
            public static Set of(Flag flag0, Flag flag1) {
                return of(flag0).with(flag1);
            }

            /**
             * Get the set optionally comprising a single flag.
             *
             * @param when {@code true} to include {@code flag}, or {@code false} to exclude it
             * @param flag the flag (must not be {@code null})
             * @return the set (not {@code null})
             */
            public static Set of(boolean when, Flag flag) {
                return when ? of(flag) : of();
            }

            /**
             * Get the set optionally comprising up to two flags.
             *
             * @param when0 {@code true} to include {@code flag0}, or {@code false} to exclude it
             * @param flag0 the first flag (must not be {@code null})
             * @param when1 {@code true} to include {@code flag1}, or {@code false} to exclude it
             * @param flag1 the second flag (must not be {@code null})
             * @return the set (not {@code null})
             */
            public static Set of(boolean when0, Flag flag0, boolean when1, Flag flag1) {
                return of(when0, flag0).with(when1, flag1);
            }

            /**
             * Get the set which is the same as this set but including the given flag.
             *
             * @param other the other flag (must not be {@code null})
             * @return the set which includes the given flag (not {@code null})
             */
            public Set with(Flag other) {
                Assert.checkNotNullParam("other", other);
                return sets[bits | (1 << other.ordinal())];
            }

            /**
             * Get the set which is the same as this set but conditionally including the given flag.
             *
             * @param when {@code true} to include {@code flag}, or {@code false} to exclude it
             * @param other the other flag (must not be {@code null})
             * @return the set which includes the given flag (not {@code null})
             */
            public Set with(boolean when, Flag other) {
                Assert.checkNotNullParam("other", other);
                return when ? sets[bits | (1 << other.ordinal())] : this;
            }

            /**
             * Get the set which is the union of this set and another set.
             *
             * @param other the other set (must not be {@code null})
             * @return the set which is the union of the two sets (not {@code null})
             */
            public Set with(Set other) {
                Assert.checkNotNullParam("other", other);
                return sets[bits | other.bits];
            }

            /**
             * Get the set which is the same as this set but without the given flag.
             *
             * @param other the other flag (must not be {@code null})
             * @return the set which excludes the given flag (not {@code null})
             */
            public Set without(Flag other) {
                Assert.checkNotNullParam("other", other);
                return sets[bits & ~(1 << other.ordinal())];
            }

            /**
             * Determine if this set contains the given flag.
             *
             * @param other the flag
             * @return {@code true} if this set contains the flag, or {@code false} if it does not or if
             *      {@code other} is {@code null}
             */
            public boolean contains(Flag other) {
                return other != null && (bits & (1 << other.ordinal())) != 0;
            }
        }
    }
}
