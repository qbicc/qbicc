package org.qbicc.plugin.wasm;

import static org.qbicc.machine.file.wasm.Ops.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Action;
import org.qbicc.graph.ActionVisitor;
import org.qbicc.graph.Add;
import org.qbicc.graph.And;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BitCast;
import org.qbicc.graph.BlockParameter;
import org.qbicc.graph.Comp;
import org.qbicc.graph.DecodeReference;
import org.qbicc.graph.EncodeReference;
import org.qbicc.graph.FpToInt;
import org.qbicc.graph.Goto;
import org.qbicc.graph.If;
import org.qbicc.graph.IntToFp;
import org.qbicc.graph.Neg;
import org.qbicc.graph.Node;
import org.qbicc.graph.Ret;
import org.qbicc.graph.Return;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Split;
import org.qbicc.graph.Switch;
import org.qbicc.graph.Terminator;
import org.qbicc.graph.TerminatorVisitor;
import org.qbicc.graph.Throw;
import org.qbicc.graph.Unreachable;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.graph.literal.EncodeReferenceLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.NullLiteral;
import org.qbicc.machine.file.wasm.FuncType;
import org.qbicc.machine.file.wasm.NumType;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.ValType;
import org.qbicc.machine.file.wasm.model.BranchTarget;
import org.qbicc.machine.file.wasm.model.DefinedFunc;
import org.qbicc.machine.file.wasm.model.InsnSeq;
import org.qbicc.machine.file.wasm.model.Local;
import org.qbicc.type.BooleanType;
import org.qbicc.type.FloatType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.NullableType;
import org.qbicc.type.NumericType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class WasmNodeVisitor implements ValueVisitor<InsnSeq, Void>, ActionVisitor<InsnSeq, Void>, TerminatorVisitor<WasmNodeVisitor.Context, Void> {
    private final CompilationContext ctxt;
    /**
     * The local in which the given value may be found.
     */
    private final Map<Value, Local> locals = new HashMap<>();
    /**
     * The set of values that should be emitted in sequence but then dropped (not stored in a local).
     */
    private final Set<Value> drops = new HashSet<>();
    private final ExecutableElement element;

    public WasmNodeVisitor(CompilationContext ctxt, ExecutableElement element) {
        this.ctxt = ctxt;
        this.element = element;
    }

    public DefinedFunc run() {
        // analyze all values for local variable assignment
        ArrayList<Local> paramsList = new ArrayList<>();
        ArrayList<Local> localsList = new ArrayList<>();
        assignLocalVars(paramsList, localsList);
        String name = ctxt.getExactNameForElement(element);
        FuncType type = mapFuncType(element.getType());
        return new DefinedFunc(name, type, paramsList, localsList, func -> {
            // map it
            doTree(element.getMethodBody().getEntryBlock(), new Context(null, null, null, func.body()));
        });
    }

    private void assignLocalVars(ArrayList<Local> paramsList, ArrayList<Local> localsList) {
        // first, assign all block parameters to fixed locals based on their type and slot, with special locals for params
        int offset = element.getType() instanceof InstanceMethodType ? 2 : 1;
        Map<ValType, Map<Slot, Local>> bpLocals = new HashMap<>();
        Set<Value> trivialValues = new HashSet<>();
        BasicBlock entryBlock = element.getMethodBody().getEntryBlock();
        List<BasicBlock> allBlocks = entryBlock.allBlocks();
        for (BasicBlock block : allBlocks) {
            Set<Slot> slots = block.getUsedParameterSlots();
            for (Slot slot : slots) {
                BlockParameter bp = block.getBlockParameter(slot);
                ValueType type = bp.getType();
                ValType mappedType = mapType(type);
                Map<Slot, Local> subMap = bpLocals.computeIfAbsent(mappedType, WasmNodeVisitor::newHashMap);
                Local local = subMap.get(slot);
                if (local == null) {
                    if (block.getIndex() == 1) {
                        local = switch (slot.kind()) {
                            case thread -> new Local(slot.toString(), mappedType, 0);
                            case this_ -> new Local(slot.toString(), mappedType, 1); // only on instance methods
                            case param -> new Local(slot.toString(), mappedType, slot.getIndex() + offset);
                            default -> throw new IllegalStateException("Unexpected slot kind " + slot.kind());
                        };
                        paramsList.add(local);
                    } else {
                        local = new Local(slot.toString(), mappedType);
                        localsList.add(local);
                    }
                    subMap.put(slot, local);
                    locals.put(bp, local);
                    // now, assign this local to all incoming values (which should be splits, mainly)
                    Set<Value> possibleValues = bp.getPossibleValues();
                    for (Value value : possibleValues) {
                        Local existing = locals.putIfAbsent(value, local);
                        if (existing != null && existing != local) {
                            throw new IllegalStateException("Conflicting local variable for " + value);
                        }
                    }
                }
            }
        }
        HashSet<Local> liveLocals = new HashSet<>();
        HashSet<Local> oldLiveLocals = new HashSet<>();
        Map<ValType, LinkedHashSet<Local>> freeLocals = new HashMap<>(32);
        // now, assign locals for the liveness duration of each value, releasing each local as it is freed
        for (BasicBlock block : allBlocks) {
            // the initial set of live locals is equal to the block entry live-out set's local vars
            liveLocals.clear();
            computeLiveLocals(block.getBlockEntry().getLiveOuts(), trivialValues, locals, liveLocals);
            // the initial set of free locals is equal to all known locals minus those locals for live values
            freeLocals.clear();
            computeFreeLocals(locals, liveLocals, freeLocals);
            for (Node node : block.getInstructions()) {
                if (node instanceof Value val) {
                    // check for some trivial cases
                    if (val instanceof Literal
                        || val instanceof Neg
                        || val instanceof Comp
                    ) {
                        trivialValues.add(val);
                        continue;
                    }
                    if (val.getType() instanceof VoidType) {
                        // technically, it doesn't need a drop, but this allows us to remember that it's not trivial
                        drops.add(val);
                        continue;
                    } else if (! val.getLiveOuts().contains(val)) {
                        // value does not survive its definition
                        drops.add(val);
                        continue;
                    }
                    // otherwise, find a local for it to live in
                    if (! locals.containsKey(val)) {
                        Local local;
                        // try to reuse one
                        ValType valType = mapType(val.getType());
                        LinkedHashSet<Local> set = freeLocals.get(valType);
                        if (set != null) {
                            // maybe...!
                            Iterator<Local> iterator = set.iterator();
                            if (iterator.hasNext()) {
                                // yes!
                                local = iterator.next();
                                iterator.remove();
                            } else {
                                // no reuse possible
                                local = new Local("", valType);
                                localsList.add(local);
                            }
                        } else {
                            // no reuse possible
                            local = new Local("", valType);
                            localsList.add(local);
                        }
                        locals.put(val, local);
                    } else {
                        // special: eliminate redundant splits
                        if (val instanceof Split split) {
                            if (locals.get(split) == locals.get(split.input())) {
                                // this will cause the split to be emitted in-place
                                locals.remove(split);
                                trivialValues.add(split);
                            }
                        }
                    }
                }
                // all values which were live but now are not can be freed into the free set
                // reuse the previous old-live set via swap operation
                HashSet<Local> tmp = oldLiveLocals;
                oldLiveLocals = liveLocals;
                liveLocals = tmp;
                computeLiveLocals(node.getLiveOuts(), trivialValues, locals, liveLocals);
                // now see what the diff is
                oldLiveLocals.removeIf(liveLocals::contains);
                // whatever is left in old is the now-dead set
                for (Local local : oldLiveLocals) {
                    freeLocals.computeIfAbsent(local.type(), WasmNodeVisitor::newLinkedHashSet).add(local);
                }
                // clear it for next iteration
                oldLiveLocals.clear();
            }
        }
    }

    private void computeFreeLocals(final Map<Value, Local> locals, final HashSet<Local> liveLocals, Map<ValType, LinkedHashSet<Local>> freeLocals) {
        for (Local x : locals.values()) {
            if (!liveLocals.contains(x)) {
                freeLocals.computeIfAbsent(x.type(), WasmNodeVisitor::newLinkedHashSet).add(x);
            }
        }
    }

    private void computeLiveLocals(final Set<Value> liveValues, final Set<Value> trivialValues, final Map<Value, Local> locals, final HashSet<Local> liveLocals) {
        for (Value live : liveValues) {
            if (trivialValues.contains(live)) {
                computeLiveLocalDeps(live, trivialValues, locals, liveLocals);
            } else if (locals.containsKey(live)) {
                liveLocals.add(locals.get(live));
            }
        }
    }

    private void computeLiveLocalDeps(final Value value, final Set<Value> trivialValues, final Map<Value, Local> locals, final HashSet<Local> liveLocals) {
        int cnt = value.getValueDependencyCount();
        for (int i = 0; i < cnt; i ++) {
            Value subVal = value.getValueDependency(i);
            if (trivialValues.contains(subVal)) {
                computeLiveLocalDeps(subVal, trivialValues, locals, liveLocals);
            } else if (locals.containsKey(subVal)) {
                liveLocals.add(locals.get(subVal));
            }
        }
    }

    private static <K, V> Map<K, V> newHashMap(Object ignored) {
        return new HashMap<>();
    }

    private static <E> LinkedHashSet<E> newLinkedHashSet(Object ignored) {
        return new LinkedHashSet<>();
    }

    // handle unknowns

    @Override
    public Void visitUnknown(InsnSeq insns, Action node) {
        throw unknownNode(node);
    }

    @Override
    public Void visitUnknown(InsnSeq insns, Value node) {
        throw unknownNode(node);
    }

    @Override
    public Void visitUnknown(Context context, Terminator node) {
        throw unknownNode(node);
    }

    @Override
    public Void visitAny(InsnSeq insns, Literal literal) {
        throw unknownNode(literal);
    }

    // literals

    @Override
    public Void visit(InsnSeq insns, BlockLiteral literal) {
        insns.add(i32.const_, literal.getBlockIndex());
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, EncodeReferenceLiteral literal) {
        map(literal.getValue(), insns);
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, IntegerLiteral literal) {
        switch (literal.getType().getMinBits()) {
            case 8, 16, 32 -> insns.add(i32.const_, literal.intValue());
            case 64 -> insns.add(i64.const_, literal.longValue());
            default -> throw badType(literal.getType());
        }
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, NullLiteral literal) {
        // all pointers are stored as integers.
        // note that function pointers are indexes into a table rather than func ref type values.
        insns.add(i32.const_, 0);
        return null;
    }

    // values

    @Override
    public Void visit(InsnSeq insns, Add node) {
        map(node.getLeftInput(), insns);
        map(node.getRightInput(), insns);
        insns.add(switch (mapArgType(node.getType())) {
            case i32 -> i32.add;
            case i64 -> i64.add;
            case f32 -> f32.add;
            case f64 -> f64.add;
            default -> throw badType(node.getType());
        });
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, And node) {
        map(node.getLeftInput(), insns);
        map(node.getRightInput(), insns);
        insns.add(switch (mapArgType(node.getType())) {
            case i32 -> i32.and;
            case i64 -> i64.and;
            default -> throw badType(node.getType());
        });
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, BitCast node) {
        map(node.getInput(), insns);
        switch (mapArgType(node.getType())) {
            case f32 -> {
                switch (mapArgType(node.getInputType())) {
                    case f32 -> {}
                    case i32 -> insns.add(f32.reinterpret_i32);
                    default -> throw badType(node.getInputType());
                }
            }
            case f64 -> {
                switch (mapArgType(node.getInputType())) {
                    case f64 -> {}
                    case i64 -> insns.add(f64.reinterpret_i64);
                    default -> throw badType(node.getInputType());
                }
            }
            case i32 -> {
                switch (mapArgType(node.getInputType())) {
                    case f32 -> insns.add(i32.reinterpret_f32);
                    case i32 -> {}
                    default -> throw badType(node.getInputType());
                }
            }
            case i64 -> {
                switch (mapArgType(node.getInputType())) {
                    case f64 -> insns.add(i64.reinterpret_f64);
                    case i64 -> {}
                    default -> throw badType(node.getInputType());
                }
            }
        }
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, Comp node) {
        map(node.getInput(), insns);
        switch (mapArgType(node.getType())) {
            case i32 -> {
                insns.add(i32.const_, -1);
                insns.add(i32.xor);
            }
            case i64 -> {
                insns.add(i64.const_, -1);
                insns.add(i64.xor);
            }
            default -> throw badType(node.getType());
        }
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, DecodeReference node) {
        // leave as-is
        map(node.getInput(), insns);
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, EncodeReference node) {
        // leave as-is
        map(node.getInput(), insns);
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, FpToInt node) {
        map(node.getInput(), insns);
        boolean signed = node.getType() instanceof SignedIntegerType;
        switch (mapArgType(node.getInputType())) {
            case f32 -> {
                switch (mapArgType(node.getType())) {
                    case i32 -> insns.add(signed ? i32.trunc_f32_s : i32.trunc_f32_u);
                    case i64 -> insns.add(signed ? i32.trunc_f64_s : i32.trunc_f64_u);
                    default -> throw badType(node.getType());
                }
            }
            case f64 -> {
                switch (mapArgType(node.getType())) {
                    case i32 -> insns.add(signed ? i64.trunc_f32_s : i64.trunc_f32_u);
                    case i64 -> insns.add(signed ? i64.trunc_f64_s : i64.trunc_f64_u);
                    default -> throw badType(node.getType());
                }
            }
            default -> throw badType(node.getInputType());
        }
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, IntToFp node) {
        map(node.getInput(), insns);
        boolean signed = node.getInputType() instanceof SignedIntegerType;
        switch (mapArgType(node.getInputType())) {
            case i32 -> {
                switch (mapArgType(node.getType())) {
                    case f32 -> insns.add(signed ? f32.convert_i32_s : f32.convert_i32_u);
                    case f64 -> insns.add(signed ? f32.convert_i64_s : f32.convert_i64_u);
                    default -> throw badType(node.getType());
                }
            }
            case i64 -> {
                switch (mapArgType(node.getType())) {
                    case f32 -> insns.add(signed ? f64.convert_i32_s : f64.convert_i32_u);
                    case f64 -> insns.add(signed ? f64.convert_i64_s : f64.convert_i64_u);
                    default -> throw badType(node.getType());
                }
            }
            default -> throw badType(node.getInputType());
        }
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, Neg node) {
        switch (mapArgType(node.getType())) {
            case i32 -> {
                insns.add(i32.const_, 0);
                map(node.getInput(), insns);
                insns.add(i32.sub);
            }
            case i64 -> {
                insns.add(i64.const_, 0);
                map(node.getInput(), insns);
                insns.add(i64.sub);
            }
            case f32 -> {
                map(node.getInput(), insns);
                insns.add(f32.neg);
            }
            case f64 -> {
                map(node.getInput(), insns);
                insns.add(f64.neg);
            }
            default -> throw badType(node.getType());
        }
        return null;
    }

    @Override
    public Void visit(InsnSeq insns, Split node) {
        // a split represents moving a value from one reg to another; we just put it on the stack here
        map(node.input(), insns);
        // our successor will then store it or use it as desired
        return null;
    }

    // control flow (terminators) - @see WasmCompatibleBasicBlockBuilder

    @Override
    public Void visit(Context context, Goto node) {
        doBranch(context.seq(), context, node.getTerminatedBlock(), node.getResumeTarget());
        return null;
    }

    @Override
    public Void visit(Context context, If node) {
        // todo: use br_if form as an optimization
        InsnSeq seq = context.seq();
        map(node.getCondition(), seq);
        BasicBlock terminatedBlock = node.getTerminatedBlock();
        doBranch(seq, context, terminatedBlock, node.getTrueBranch());
        seq.add(else_);
        doBranch(seq, context, terminatedBlock, node.getFalseBranch());
        seq.end();
        return null;
    }

    @Override
    public Void visit(Context context, Ret node) {
        // implement ret using switch or if
        InsnSeq seq = context.seq();
        Value rav = node.getReturnAddressValue();
        if (! (rav instanceof BlockParameter bp)) {
            throw new IllegalStateException("Expected finite value possibilities");
        }
        // push the return address (block ID)
        map(rav, seq);
        List<BasicBlock> targets = bp.getPossibleValues().stream().map(BlockLiteral.class::cast).map(BlockLiteral::getBlock).sorted(Comparator.comparingInt(BasicBlock::getIndex)).toList();
        if (targets.isEmpty()) {
            seq.add(unreachable);
        } else if (targets.size() == 1) {
            // just one option
            seq.add(br, context.get(targets.get(0)));
        } else if (targets.size() == 2) {
            BasicBlock target0 = targets.get(0);
            seq.add(i32.const_, target0.getIndex());
            seq.add(i32.eq);
            seq.add(br_if, context.get(target0));
            seq.add(br, context.get(targets.get(1)));
        } else {
            // table time
            BasicBlock default_ = targets.get(0);
            int least = targets.get(1).getIndex();
            // offset (`least` is never zero)
            seq.add(i32.const_, least);
            seq.add(i32.sub);
            int greatest = targets.get(targets.size() - 1).getIndex();
            BasicBlock[] allTargets = new BasicBlock[greatest - least];
            Arrays.fill(allTargets, default_);
            for (BasicBlock target : targets) {
                allTargets[target.getIndex() - least] = target;
            }
            seq.add(br_table, Stream.of(allTargets).map(context::get).toList(), context.get(default_));
        }
        return null;
    }

    @Override
    public Void visit(Context context, Return node) {
        InsnSeq seq = context.seq();
        if (! (node.getReturnValue().getType() instanceof VoidType)) {
            map(node.getReturnValue(), seq);
        }
        seq.add(return_);
        return null;
    }

    @Override
    public Void visit(Context context, Switch node) {
        // this translation relies on a reasonably dense switch
        InsnSeq seq = context.seq();
        map(node.getSwitchValue(), seq);
        int valCnt = node.getNumberOfValues();
        int least = node.getValueForIndex(0);
        int greatest = node.getValueForIndex(valCnt - 1);
        if (least != 0) {
            // offset
            seq.add(i32.const_, least);
            seq.add(i32.sub);
        }
        // we can map directly, because `nodeWithin` will automatically flatten switch statements
        BranchTarget defaultTarget = context.get(node.getDefaultTarget());
        // todo: this runs in O(n log₂ n) time (gTFV is a binary search), but an O(n) impl is possible
        List<BranchTarget> targets = IntStream.range(0, greatest - least).mapToObj(
            val -> context.get(Objects.requireNonNullElse(node.getTargetForValue(val + least), node.getDefaultTarget()))
        ).toList();
        seq.add(br_table, targets, defaultTarget);
        return null;
    }

    @Override
    public Void visit(Context context, Throw node) {
        // for now...
        context.seq().add(unreachable);
        return null;
    }

    @Override
    public Void visit(Context context, Unreachable node) {
        context.seq().add(unreachable);
        return null;
    }

    // structuralization

    private static boolean isBackward(final BasicBlock fromBlock, final BasicBlock toBlock) {
        return toBlock.getIndex() < fromBlock.getIndex();
    }

    private static boolean isMergeNode(final BasicBlock block) {
        return block.forwardIncomingEdgeCount() > 1;
    }

    private void doTree(BasicBlock x, Context context) {
        if (x.hasBackIncomingEdge()) {
            // it's a loop header
            context.seq().add(loop, insn -> {
                nodeWithin(x, context.with(x, insn, insn.body()));
                insn.body().end();
            });
        } else {
            nodeWithin(x, context);
        }
    }

    private static final Set<Class<? extends Terminator>> specials = Set.of(
        Switch.class,
        Ret.class
    );

    private void nodeWithin(BasicBlock x, Context context) {
        // for each merge-node child, recursively place blocks
        // todo: should this be successors...?
        List<BasicBlock> mergeChildren = specials.contains(x.getClass()) ? x.dominatedStream().toList() : x.dominatedStream().filter(WasmNodeVisitor::isMergeNode).toList();
        if (mergeChildren.isEmpty()) {
            emitInstructions(x, context);
        } else {
            for (BasicBlock mergeChild : mergeChildren) {
                context.seq().add(block, insn -> {
                    nodeWithin(mergeChild, context.with(mergeChild, insn, insn.body()));
                    // use the outer context to render the body of this child, so branches leave the predecessor
                    doTree(mergeChild, context);
                });
            }
        }
    }

    private void doBranch(InsnSeq seq, Context linkMap, BasicBlock fromBlock, BasicBlock toBlock) {
        if (isBackward(fromBlock, toBlock)) {
            // the destination is some previous (possibly loop-wrapped) block
            seq.add(br, linkMap.get(toBlock));
        } else if (isMergeNode(toBlock)) {
            // the destination is a merge node
            seq.add(br, linkMap.get(toBlock));
        } else {
            // fall out forwards
            doTree(toBlock, linkMap);
        }
    }

    private void emitInstructions(final BasicBlock block, final Context context) {
        InsnSeq seq = context.seq();
        for (Node instruction : block.getInstructions()) {
            if (instruction instanceof Value v) {
                if (locals.containsKey(v)) {
                    v.accept(this, seq);
                    seq.add(local.set, locals.get(v));
                } else if (drops.contains(v)) {
                    // no local, but still emit in sequence
                    v.accept(this, seq);
                    if (! (v.getType() instanceof VoidType)) {
                        // result value is not used
                        seq.add(drop);
                    }
                }
                // otherwise, the value is emitted at each use site!
            } else if (instruction instanceof Action a) {
                a.accept(this, seq);
            } else if (instruction instanceof Terminator t) {
                t.accept(this, context);
            }
        }
    }

    /**
     * The control flow context.
     */
    public static final class Context {
        private final Context prev;
        private final BasicBlock key;
        private final BranchTarget value;
        private final InsnSeq seq;

        Context(Context prev, BasicBlock key, BranchTarget value, InsnSeq seq) {
            this.prev = prev;
            this.key = key;
            this.value = value;
            this.seq = seq;
        }

        public Context prev() {
            return prev;
        }

        public BasicBlock key() {
            return key;
        }

        public BranchTarget value() {
            return value;
        }

        public InsnSeq seq() {
            return seq;
        }

        public Context with(InsnSeq seq) {
            return new Context(this, key, value, seq);
        }

        public Context with(BasicBlock key, BranchTarget value) {
            return new Context(this, key, value, seq);
        }

        public Context with(BasicBlock key, BranchTarget value, InsnSeq seq) {
            return new Context(this, key, value, seq);
        }

        public BranchTarget get(BasicBlock key) {
            if (key == this.key) {
                return this.value;
            } else if (prev != null) {
                return prev.get(key);
            } else {
                throw new NoSuchElementException(key.toString());
            }
        }
    }

    private void map(Value value, InsnSeq insns) {
        if (locals.containsKey(value)) {
            // the value is in a local var; load it
            insns.add(local.get, locals.get(value));
        } else {
            // produce the node directly at this location
            value.accept(this, insns);
        }
    }

    private static ArgType mapArgType(ValueType type) {
        if (type instanceof IntegerType it) {
            return switch (it.getMinBits()) {
                case 8, 16, 32 -> ArgType.i32;
                case 64 -> ArgType.i64;
                default -> throw badType(type);
            };
        } else if (type instanceof FloatType ft) {
            return switch (ft.getMinBits()) {
                case 32 -> ArgType.f32;
                case 64 -> ArgType.f64;
                default -> throw badType(type);
            };
        } else if (type instanceof BooleanType) {
            return ArgType.i32;
        } else if (type instanceof NullableType nt) {
            return switch (nt.getMinBits()) {
                case 32 -> ArgType.i32;
                case 64 -> ArgType.i64;
                default -> throw badType(type);
            };
        } else {
            throw badType(type);
        }
    }

    enum ArgType {
        i32,
        i64,
        f32,
        f64,
        funcRef,
        externalRef,
    }

    // TODO: add thread & this arguments
    public FuncType mapFuncType(InvokableType type) {
        List<ValueType> parameterTypes = type.getParameterTypes();
        ValueType returnType = type.getReturnType();
        if (returnType instanceof VoidType) {
            if (parameterTypes.isEmpty()) {
                return FuncType.EMPTY;
            } else {
                return new FuncType(parameterTypes.stream().map(WasmNodeVisitor::mapType).toList(), List.of());
            }
        } else {
            if (parameterTypes.isEmpty()) {
                return FuncType.returning(mapType(returnType));
            } else {
                return new FuncType(parameterTypes.stream().map(WasmNodeVisitor::mapType).toList(), List.of(mapType(returnType)));
            }
        }
    }

    public static NumType mapNumType(NumericType type) {
        return switch (mapArgType(type)) {
            case i32 -> NumType.i32;
            case i64 -> NumType.i64;
            case f32 -> NumType.f32;
            case f64 -> NumType.f64;
            default -> throw badType(type);
        };
    }

    public static ValType mapType(ValueType type) {
        return switch (mapArgType(type)) {
            case i32 -> NumType.i32;
            case i64 -> NumType.i64;
            case f32 -> NumType.f32;
            case f64 -> NumType.f64;
            case funcRef -> RefType.funcref;
            case externalRef -> RefType.externref;
        };
    }

    // exceptions

    private static IllegalStateException badType(ValueType type) {
        return new IllegalStateException("Bad node type " + type);
    }

    private static IllegalStateException unknownNode(Node node) {
        return new IllegalStateException("Unknown node " + node);
    }


}
