package org.qbicc.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.TypeIdLiteral;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FloatType;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.StructType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NullableType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeIdType;
import org.qbicc.type.UnionType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.methodhandle.MethodMethodHandleConstant;

final class SimpleBasicBlockBuilder implements BasicBlockBuilder {
    private BlockLabel firstBlock;
    private int line;
    private int bci;
    private Node dependency;
    private BlockEntry blockEntry;
    private BlockLabel currentBlock;
    private BasicBlockBuilder firstBuilder;
    private ExecutableElement element;
    private final ExecutableElement rootElement;
    private ProgramLocatable callSite;
    private BasicBlock terminatedBlock;
    private final Map<BlockLabel, Map<Slot, BlockParameter>> parameters;
    private final Map<Value, Value> unique = new HashMap<>();

    SimpleBasicBlockBuilder(final ExecutableElement element) {
        this.element = element;
        this.rootElement = element;
        bci = - 1;
        parameters = new HashMap<>();
    }

    @Override
    public BlockParameter addParam(BlockLabel owner, Slot slot, ValueType type, boolean nullable) {
        Map<Slot, BlockParameter> subMap = parameters.computeIfAbsent(owner, SimpleBasicBlockBuilder::newMap);
        BlockParameter parameter = subMap.get(slot);
        if (parameter != null) {
            if (parameter.getSlot().equals(slot) && parameter.getType().equals(type) && parameter.isNullable() == nullable) {
                return parameter;
            }
            throw new IllegalArgumentException("Parameter " + slot + " already defined to " + owner);
        }
        if (nullable && ! (type instanceof NullableType)) {
            throw new IllegalArgumentException("Parameter can only be nullable if its type is nullable");
        }
        if (type instanceof VoidType) {
            throw new IllegalArgumentException("Void-typed block parameter");
        }
        parameter = new BlockParameter(this, type, nullable, owner, slot);
        subMap.put(slot, parameter);
        return parameter;
    }

    @Override
    public BlockParameter getParam(BlockLabel owner, Slot slot) throws NoSuchElementException {
        BlockParameter parameter = parameters.getOrDefault(owner, Map.of()).get(slot);
        if (parameter == null) {
            throw new NoSuchElementException("No parameter for slot " + slot + " in " + owner);
        }
        return parameter;
    }

    public BasicBlockBuilder getFirstBuilder() {
        return firstBuilder;
    }

    public void setFirstBuilder(final BasicBlockBuilder first) {
        firstBuilder = Assert.checkNotNullParam("first", first);
    }

    public ExecutableElement element() {
        return element;
    }

    public ExecutableElement getRootElement() { return rootElement; }

    public ExecutableElement setCurrentElement(final ExecutableElement element) {
        ExecutableElement old = this.element;
        this.element = element;
        return old;
    }

    public ProgramLocatable callSite() {
        return callSite;
    }

    public ProgramLocatable setCallSite(final ProgramLocatable callSite) {
        ProgramLocatable old = this.callSite;
        this.callSite = callSite;
        return old;
    }

    public Location getLocation() {
        return Location.builder()
            .setElement(element)
            .setLineNumber(line)
            .setByteCodeIndex(bci)
            .build();
    }

    public int setLineNumber(final int newLineNumber) {
        try {
            return line;
        } finally {
            line = newLineNumber;
        }
    }

    @Override
    public int lineNumber() {
        return line;
    }

    public int setBytecodeIndex(final int newBytecodeIndex) {
        try {
            return bci;
        } finally {
            bci = newBytecodeIndex;
        }
    }

    public int bytecodeIndex() {
        return bci;
    }

    public void finish() {
        if (currentBlock != null) {
            throw new IllegalStateException("Current block not terminated");
        }
        if (firstBlock != null) {
            mark(BlockLabel.getTargetOf(firstBlock), null);
            computeLoops(BlockLabel.getTargetOf(firstBlock), new ArrayList<>(), new HashSet<>(), new HashSet<>(), new HashMap<>());
            getContext().getScheduler().schedule(getFirstBlock());
        }
    }

    @Override
    public BasicBlock getFirstBlock() throws IllegalStateException {
        BlockLabel firstBlock = this.firstBlock;
        if (firstBlock != null && firstBlock.hasTarget()) {
            return BlockLabel.getTargetOf(firstBlock);
        }
        throw new IllegalStateException("First block not yet terminated");
    }

    @Override
    public BlockLabel getEntryLabel() throws IllegalStateException {
        BlockLabel firstBlock = this.firstBlock;
        if (firstBlock != null) {
            return firstBlock;
        }
        throw new IllegalStateException("First block not yet started");
    }

    private void mark(BasicBlock block, BasicBlock from) {
        if (block.setReachableFrom(from)) {
            Terminator terminator = block.getTerminator();
            int cnt = terminator.getSuccessorCount();
            for (int i = 0; i < cnt; i ++) {
                mark(terminator.getSuccessor(i), block);
            }
        }
    }

    private void computeLoops(BasicBlock block, ArrayList<BasicBlock> blocks, HashSet<BasicBlock> blocksSet, HashSet<BasicBlock> visited, Map<Set<BasicBlock.Loop>, Map<BasicBlock.Loop, Set<BasicBlock.Loop>>> cache) {
        if (! visited.add(block)) {
            return;
        }
        blocks.add(block);
        blocksSet.add(block);
        Terminator terminator = block.getTerminator();
        int cnt = terminator.getSuccessorCount();
        for (int i = 0; i < cnt; i ++) {
            BasicBlock successor = terminator.getSuccessor(i);
            if (blocksSet.contains(successor)) {
                int idx = blocks.indexOf(successor);
                assert idx != -1;
                // all blocks in the span are a part of the new loop
                BasicBlock.Loop loop = new BasicBlock.Loop(successor, block);
                for (int j = idx; j < blocks.size(); j ++) {
                    BasicBlock member = blocks.get(j);
                    Set<BasicBlock.Loop> oldLoops = member.getLoops();
                    member.setLoops(cache.computeIfAbsent(oldLoops, SimpleBasicBlockBuilder::newMap).computeIfAbsent(loop, l -> setWith(oldLoops, l)));
                }
            } else {
                // a block we haven't hit yet
                computeLoops(successor, blocks, blocksSet, visited, cache);
            }
        }
        BasicBlock removed = blocks.remove(blocks.size() - 1);
        assert removed == block;
        blocksSet.remove(block);
    }

    private static <K, V> Map<K, V> newMap(Object arg) {
        return new HashMap<>();
    }

    private static <E> Set<E> setWith(Set<E> set, E item) {
        if (set.contains(item)) {
            return set;
        }
        int size = set.size();
        if (size == 0) {
            return Set.of(item);
        } else if (size == 1) {
            return Set.of(set.iterator().next(), item);
        } else if (size == 2) {
            Iterator<E> iterator = set.iterator();
            return Set.of(iterator.next(), iterator.next(), item);
        } else {
            @SuppressWarnings("unchecked")
            E[] array = set.toArray((E[]) new Object[size + 1]);
            array[size] = item;
            return Set.of(array);
        }
    }

    public Value add(final Value v1, final Value v2) {
        return unique(new Add(this, v1, v2));
    }

    public Value multiply(final Value v1, final Value v2) {
        return unique(new Multiply(this, v1, v2));
    }

    public Value and(final Value v1, final Value v2) {
        return unique(new And(this, v1, v2));
    }

    public Value or(final Value v1, final Value v2) {
        return unique(new Or(this, v1, v2));
    }

    public Value xor(final Value v1, final Value v2) {
        return unique(new Xor(this, v1, v2));
    }

    public Value isEq(final Value v1, final Value v2) {
        return unique(new IsEq(this, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value isNe(final Value v1, final Value v2) {
        return unique(new IsNe(this, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value shr(final Value v1, final Value v2) {
        return unique(new Shr(this, v1, v2));
    }

    public Value shl(final Value v1, final Value v2) {
        return unique(new Shl(this, v1, v2));
    }

    public Value sub(final Value v1, final Value v2) {
        return unique(new Sub(this, v1, v2));
    }

    public Value divide(final Value v1, final Value v2) {
        return unique(new Div(this, v1, v2, requireDependency()));
    }

    public Value remainder(final Value v1, final Value v2) {
        return unique(new Mod(this, v1, v2, requireDependency()));
    }

    public Value min(final Value v1, final Value v2) {
        return unique(new Min(this, v1, v2));
    }

    public Value max(final Value v1, final Value v2) {
        return unique(new Max(this, v1, v2));
    }

    public Value isLt(final Value v1, final Value v2) {
        return unique(new IsLt(this, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value isGt(final Value v1, final Value v2) {
        return unique(new IsGt(this, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value isLe(final Value v1, final Value v2) {
        return unique(new IsLe(this, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value isGe(final Value v1, final Value v2) {
        return unique(new IsGe(this, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value rol(final Value v1, final Value v2) {
        return unique(new Rol(this, v1, v2));
    }

    public Value ror(final Value v1, final Value v2) {
        return unique(new Ror(this, v1, v2));
    }

    public Value cmp(Value v1, Value v2) {
        return unique(new Cmp(this, v1, v2, getTypeSystem().getSignedInteger32Type()));
    }

    public Value cmpG(Value v1, Value v2) {
        return unique(new CmpG(this, v1, v2, getTypeSystem().getSignedInteger32Type()));
    }

    public Value cmpL(Value v1, Value v2) {
        return unique(new CmpL(this, v1, v2, getTypeSystem().getSignedInteger32Type()));
    }

    public Value split(final Value value) {
        return unique(new Split(this, value));
    }

    public Value notNull(Value v) {
        return v.isNullable() ? unique(new NotNull(this, v)) : v;
    }

    public Value negate(final Value v) {
        return unique(new Neg(this, v));
    }

    public Value complement(Value v) {
        Assert.checkNotNullParam("v", v);
        if (! (v.getType() instanceof IntegerType || v.getType() instanceof BooleanType)) {
            throw new IllegalArgumentException("Invalid input type");
        }
        return unique(new Comp(this, v));
    }

    public Value byteSwap(final Value v) {
        return unique(new ByteSwap(this, v));
    }

    public Value bitReverse(final Value v) {
        return unique(new BitReverse(this, v));
    }

    public Value countLeadingZeros(final Value v) {
        return unique(new CountLeadingZeros(this, v, getTypeSystem().getSignedInteger32Type()));
    }

    public Value countTrailingZeros(final Value v) {
        return unique(new CountTrailingZeros(this, v, getTypeSystem().getSignedInteger32Type()));
    }

    public Value populationCount(final Value v) {
        return unique(new PopCount(this, v, getTypeSystem().getSignedInteger32Type()));
    }

    public Value loadLength(final Value arrayPointer) {
        throw new IllegalStateException("loadLength not converted");
    }

    public Value loadTypeId(final Value objectPointer) {
        throw new IllegalStateException("loadTypeId not converted");
    }

    public Value truncate(final Value value, final WordType toType) {
        return unique(new Truncate(this, value, toType));
    }

    public Value extend(final Value value, final WordType toType) {
        return unique(new Extend(this, value, toType));
    }

    public Value bitCast(final Value value, final WordType toType) {
        return unique(new BitCast(this, value, toType));
    }

    public Value fpToInt(final Value value, final IntegerType toType) {
        return unique(new FpToInt(this, value, toType));
    }

    public Value intToFp(final Value value, final FloatType toType) {
        return unique(new IntToFp(this, value, toType));
    }

    public Value decodeReference(Value refVal, PointerType pointerType) {
        // not asDependency() because the dependency may precede the required dependency
        return unique(new DecodeReference(this, requireDependency(), refVal, pointerType));
    }

    public Value encodeReference(Value pointer, ReferenceType referenceType) {
        // not asDependency() because the dependency may precede the required dependency
        return unique(new EncodeReference(this, requireDependency(), pointer, referenceType));
    }

    public Value instanceOf(final Value input, final ObjectType expectedType, final int expectedDimensions) {
        final BasicBlockBuilder fb = getFirstBuilder();
        ObjectType ifTrueExpectedType = expectedType;
        for (int i=0; i<expectedDimensions; i++) {
            ifTrueExpectedType = ifTrueExpectedType.getReferenceArrayObject();
        }
        ReferenceType narrowed = input.getType(ReferenceType.class).narrow(ifTrueExpectedType);
        if (narrowed == null) {
            throw new IllegalStateException(String.format("Invalid instanceof check from %s to %s", input.getType(), expectedType));
        }
        return asDependency(new InstanceOf(this, requireDependency(), input, fb.notNull(fb.bitCast(input, narrowed)), expectedType, expectedDimensions, getTypeSystem().getBooleanType()));
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        throw new IllegalStateException("InstanceOf of unresolved type");
    }

    public Value checkcast(final Value value, final Value toType, final Value toDimensions, final CheckCast.CastType kind, final ObjectType expectedType) {
        ValueType inputType = value.getType();
        if (inputType instanceof VoidType) {
            return value;
        }
        if (! (inputType instanceof ReferenceType)) {
            getContext().error(getLocation(), "Only references can be checkcast (got type %s)", inputType);
            throw new BlockEarlyTermination(unreachable());
        }
        ValueType toTypeTypeRaw = toType.getType();
        if (! (toTypeTypeRaw instanceof TypeIdType)) {
            throw new IllegalArgumentException("Invalid type for toType argument");
        }
        ReferenceType outputType = ((ReferenceType) inputType).narrow(expectedType);
        if (outputType == null) {
            throw new IllegalStateException(String.format("Invalid cast from %s to %s", inputType, expectedType));
        }
        return asDependency(new CheckCast(this, requireDependency(), value, toType, toDimensions, kind, expectedType));
    }

    public Value checkcast(final Value value, final TypeDescriptor desc) {
        throw new IllegalStateException("CheckCast of unresolved type");
    }

    public Value deref(Value pointer) {
        return unique(new Dereference(this, pointer));
    }

    public Value invokeDynamic(MethodMethodHandleConstant bootstrapHandle, List<Literal> bootstrapArgs, String name, MethodDescriptor descriptor, List<Value> arguments) {
        getContext().error(getLocation(), "Unhandled `invokeDynamic`");
        throw new BlockEarlyTermination(unreachable());
    }

    public Value currentThread() {
        ReferenceType refType = element.getEnclosingType().getContext().getCompilationContext().getBootstrapClassContext().findDefinedType("java/lang/Thread").load().getObjectType().getReference();
        return unique(new CurrentThread(this, refType));
    }

    public Value vaArg(Value vaList, ValueType type) {
        return asDependency(new VaArg(this, requireDependency(), vaList, type));
    }

    public Value memberOf(final Value structPointer, final StructType.Member member) {
        return unique(new MemberOf(this, structPointer, member));
    }

    public Value memberOfUnion(Value unionPointer, UnionType.Member member) {
        return unique(new MemberOfUnion(this, unionPointer, member));
    }

    public Value elementOf(final Value arrayPointer, final Value index) {
        return unique(new ElementOf(this, arrayPointer, index));
    }

    public Value offsetPointer(Value basePointer, Value offset) {
        return unique(new OffsetPointer(this, basePointer, offset));
    }

    public Value pointerDifference(final Value leftPointer, final Value rightPointer) {
        return unique(new PointerDifference(this, leftPointer, rightPointer));
    }

    public Value byteOffsetPointer(Value base, Value offset, ValueType outputType) {
        return unique(new ByteOffsetPointer(this, base, offset, outputType));
    }

    @Override
    public Value lookupVirtualMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("lookupVirtualMethod of unresolved type");
    }

    @Override
    public Value lookupVirtualMethod(Value reference, InstanceMethodElement method) {
        return unique(new VirtualMethodLookup(this, requireDependency(), reference, method));
    }

    @Override
    public Value lookupInterfaceMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("lookupInterfaceMethod of unresolved type");
    }

    @Override
    public Value lookupInterfaceMethod(Value reference, InstanceMethodElement method) {
        return unique(new InterfaceMethodLookup(this, requireDependency(), reference, method));
    }

    @Override
    public Value resolveStaticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("resolveStaticMethod of unresolved type");
    }

    @Override
    public Value resolveInstanceMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("resolveInstanceMethod of unresolved type");
    }

    @Override
    public Value resolveConstructor(TypeDescriptor owner, MethodDescriptor descriptor) {
        throw new IllegalStateException("resolveConstructor of unresolved type");
    }

    @Override
    public Value threadBound(Value threadPtr, Value target) {
        return unique(new ThreadBound(this, threadPtr, target));
    }

    @Override
    public Value receiverBound(Value boundReceiver, Value methodPointer) {
        return unique(new ReceiverBound(this, boundReceiver, methodPointer));
    }

    public Value resolveStaticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Static field of unresolved type");
    }

    public Value instanceFieldOf(Value instancePointer, InstanceFieldElement field) {
        return unique(new InstanceFieldOf(this, instancePointer, field));
    }

    public Value instanceFieldOf(Value instancePointer, TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Instance field of unresolved type");
    }

    public Value auto(Value initializer) {
        return unique(new Auto(this, requireDependency(), initializer));
    }

    public Value stackAllocate(final ValueType type, final Value count, final Value align) {
        return asDependency(new StackAllocation(this, requireDependency(), type, count, align));
    }

    public Value offsetOfField(FieldElement fieldElement) {
        return unique(new OffsetOfField(this, fieldElement, getTypeSystem().getSignedInteger32Type()));
    }

    public Value extractElement(Value array, Value index) {
        return unique(new ExtractElement(this, array, index));
    }

    public Value extractMember(Value compound, StructType.Member member) {
        return unique(new ExtractMember(this, compound, member));
    }

    public Value extractInstanceField(Value valueObj, TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Field access of unresolved class");
    }

    public Value extractInstanceField(Value valueObj, FieldElement field) {
        return new ExtractInstanceField(this, valueObj, field, field.getType());
    }

    public Value insertElement(Value array, Value index, Value value) {
        return unique(new InsertElement(this, array, index, value));
    }

    public Value insertMember(Value compound, StructType.Member member, Value value) {
        return unique(new InsertMember(this, compound, value, member));
    }

    public Node declareDebugAddress(LocalVariableElement variable, Value address) {
        return asDependency(new DebugAddressDeclaration(this, requireDependency(), variable, address));
    }

    public Node setDebugValue(LocalVariableElement variable, Value value) {
        return asDependency(new DebugValueDeclaration(this, requireDependency(), variable, value));
    }

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        return unique(new Select(this, condition, trueValue, falseValue));
    }

    public Value classOf(Value typeId, Value dimensions) {
        Assert.assertTrue(typeId instanceof TypeIdLiteral);
        ClassContext classContext = element.getEnclosingType().getContext();
        ClassObjectType type = classContext.findDefinedType("java/lang/Class").load().getClassType();
        return unique(new ClassOf(this, typeId, dimensions, type.getReference()));
    }

    public Value new_(final ClassObjectType type, final Value typeId, final Value size, final Value align) {
        return asDependency(new New(this, requireDependency(), type, typeId, size, align));
    }

    public Value new_(final ClassTypeDescriptor desc) {
        throw new IllegalStateException("New of unresolved class");
    }

    public Value newArray(final PrimitiveArrayObjectType arrayType, final Value size) {
        return asDependency(new NewArray(this, requireDependency(), arrayType, size));
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        throw new IllegalStateException("New of unresolved array type");
    }

    public Value newReferenceArray(final ReferenceArrayObjectType arrayType, Value elemTypeId, Value dimensions, final Value size) {
        return asDependency(new NewReferenceArray(this, requireDependency(), arrayType, elemTypeId, dimensions, size));
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        return asDependency(new MultiNewArray(this, requireDependency(), arrayType, dimensions));
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        throw new IllegalStateException("New of unresolved array type");
    }

    public Value load(final Value pointer, final ReadAccessMode mode) {
        return asDependency(new Load(this, requireDependency(), pointer, mode));
    }

    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return asDependency(new ReadModifyWrite(this, requireDependency(), pointer, op, update, readMode, writeMode));
    }

    public Value cmpAndSwap(Value pointer, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        CompilationContext ctxt = element().getEnclosingType().getContext().getCompilationContext();
        return asDependency(new CmpAndSwap(this, CmpAndSwap.getResultType(ctxt, pointer.getPointeeType()), requireDependency(), pointer, expect, update, readMode, writeMode, strength));
    }

    public Node store(Value handle, Value value, WriteAccessMode mode) {
        return asDependency(new Store(this, requireDependency(), handle, value, mode));
    }

    public Node initCheck(InitializerElement initializer, Value initThunk) {
        return asDependency(new InitCheck(this, requireDependency(), initializer, initThunk));
    }

    public Node initializeClass(final Value value) {
        return asDependency(new InitializeClass(this, requireDependency(), value));
    }

    public Node fence(final GlobalAccessMode fenceType) {
        return asDependency(new Fence(this, requireDependency(), fenceType));
    }

    public Node monitorEnter(final Value obj) {
        return asDependency(new MonitorEnter(this, requireDependency(), Assert.checkNotNullParam("obj", obj)));
    }

    public Node monitorExit(final Value obj) {
        return asDependency(new MonitorExit(this, requireDependency(), Assert.checkNotNullParam("obj", obj)));
    }

    @Override
    public Value nullCheck(Value input) {
        throw new IllegalStateException("nullCheck() not intercepted");
    }

    @Override
    public Value divisorCheck(Value input) {
        throw new IllegalStateException("divisorCheck() not intercepted");
    }

    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        checkReceiver(targetPtr, receiver);
        return asDependency(new Call(this, requireDependency(), targetPtr, receiver, arguments));
    }

    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        checkReceiver(targetPtr, receiver);
        return unique(new CallNoSideEffects(this, targetPtr, receiver, arguments));
    }

    public Node nop() {
        return requireDependency();
    }

    private <N extends Node> N asDependency(N node) {
        this.dependency = node;
        return node;
    }

    private <V extends Value> V unique(V value) {
        Value existing = unique.putIfAbsent(value, value);
        //noinspection unchecked
        return existing != null ? (V) existing : value;
    }

    public Node begin(final BlockLabel blockLabel) {
        Assert.checkNotNullParam("blockLabel", blockLabel);
        if (blockLabel.hasTarget()) {
            throw new IllegalStateException("Block already terminated");
        }
        if (currentBlock != null) {
            throw new IllegalStateException("Block already in progress");
        }
        currentBlock = blockLabel;
        if (firstBlock == null) {
            firstBlock = blockLabel;
        }
        return dependency = blockEntry = new BlockEntry(this, blockLabel);
    }

    @Override
    public <T> BasicBlock begin(BlockLabel blockLabel, T arg, BiConsumer<T, BasicBlockBuilder> maker) {
        Assert.checkNotNullParam("blockLabel", blockLabel);
        Assert.checkNotNullParam("maker", maker);
        if (blockLabel.hasTarget()) {
            throw new IllegalStateException("Block already terminated");
        }
        // save all state on the stack
        final int oldLine = line;
        final int oldBci = bci;
        final Node oldDependency = dependency;
        final BlockEntry oldBlockEntry = blockEntry;
        final BlockLabel oldCurrentBlock = currentBlock;
        final BasicBlock oldTerminatedBlock = terminatedBlock;
        final ExecutableElement oldElement = element;
        final ProgramLocatable oldCallSite = callSite;
        try {
            return doBegin(blockLabel, arg, maker);
        } finally {
            // restore all state
            callSite = oldCallSite;
            element = oldElement;
            terminatedBlock = oldTerminatedBlock;
            currentBlock = oldCurrentBlock;
            blockEntry = oldBlockEntry;
            dependency = oldDependency;
            bci = oldBci;
            line = oldLine;
        }
    }

    private <T> BasicBlock doBegin(final BlockLabel blockLabel, final T arg, final BiConsumer<T, BasicBlockBuilder> maker) {
        try {
            currentBlock = blockLabel;
            if (firstBlock == null) {
                firstBlock = blockLabel;
            }
            dependency = blockEntry = new BlockEntry(this, blockLabel);
            maker.accept(arg, firstBuilder);
            if (currentBlock != null) {
                getContext().error(getLocation(), "Block not terminated");
                firstBuilder.unreachable();
            }
        } catch (BlockEarlyTermination bet) {
            blockLabel.setTarget(bet.getTerminatedBlock());
        }
        return BlockLabel.getTargetOf(blockLabel);
    }

    public Node reachable(final Value value) {
        return asDependency(new Reachable(this, requireDependency(), value));
    }

    public Node pollSafePoint() {
        return asDependency(new PollSafePoint(this, requireDependency()));
    }

    public Node enterSafePoint(Value setBits, Value clearBits) {
        return asDependency(new EnterSafePoint(this, requireDependency(), setBits, clearBits));
    }

    public Node exitSafePoint(Value setBits, Value clearBits) {
        return asDependency(new ExitSafePoint(this, requireDependency(), setBits, clearBits));
    }

    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        checkReceiver(targetPtr, receiver);
        return terminate(requireCurrentBlock(), new CallNoReturn(this, blockEntry, dependency, targetPtr, receiver, arguments));
    }

    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        checkReceiver(targetPtr, receiver);
        return terminate(requireCurrentBlock(), new InvokeNoReturn(this, blockEntry, dependency, targetPtr, receiver, arguments, catchLabel, targetArguments));
    }

    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        checkReceiver(targetPtr, receiver);
        return terminate(requireCurrentBlock(), new TailCall(this, blockEntry, dependency, targetPtr, receiver, arguments));
    }

    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        checkReceiver(targetPtr, receiver);
        final BlockLabel currentBlock = requireCurrentBlock();
        Invoke invoke = new Invoke(this, blockEntry, dependency, targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
        terminate(currentBlock, invoke);
        // todo: addParam(resumeLabel, Slot.result(), targetPtr.getReturnType())
        return invoke.getReturnValue();
    }

    public BasicBlock goto_(final BlockLabel resumeLabel, final Map<Slot, Value> targetArguments) {
        return terminate(requireCurrentBlock(), new Goto(this, blockEntry, dependency, resumeLabel, targetArguments));
    }

    public BasicBlock if_(final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget, final Map<Slot, Value> targetArguments) {
        return terminate(requireCurrentBlock(), new If(this, blockEntry, dependency, condition, trueTarget, falseTarget, targetArguments));
    }

    public BasicBlock return_(final Value value) {
        if (value == null) {
            return return_(emptyVoid());
        }
        return terminate(requireCurrentBlock(), new Return(this, blockEntry, dependency, value));
    }

    public BasicBlock unreachable() {
        return terminate(requireCurrentBlock(), new Unreachable(this, blockEntry, dependency));
    }

    public BasicBlock throw_(final Value value) {
        return terminate(requireCurrentBlock(), new Throw(this, blockEntry, dependency, value));
    }

    public BasicBlock ret(final Value address, Map<Slot, Value> targetArguments) {
        return terminate(requireCurrentBlock(), new Ret(this, blockEntry, dependency, address, targetArguments));
    }

    public BlockEntry getBlockEntry() {
        requireCurrentBlock();
        return blockEntry;
    }

    public BasicBlock getTerminatedBlock() {
        BasicBlock block = terminatedBlock;
        if (block == null) {
            throw new IllegalStateException("No block terminated yet");
        }
        return block;
    }

    public BasicBlock switch_(final Value value, final int[] checkValues, final BlockLabel[] targets, final BlockLabel defaultTarget, final Map<Slot, Value> targetArguments) {
        return terminate(requireCurrentBlock(), new Switch(this, blockEntry, dependency, defaultTarget, checkValues, targets, value, targetArguments));
    }

    private BasicBlock terminate(final BlockLabel block, final Terminator op) {
        BasicBlock realBlock = op.getTerminatedBlock();
        terminatedBlock = realBlock;
        block.setTarget(realBlock);
        blockEntry = null;
        currentBlock = null;
        dependency = null;
        return realBlock;
    }

    private BlockLabel requireCurrentBlock() {
        BlockLabel block = this.currentBlock;
        if (block == null) {
            assert dependency == null;
            throw noBlock();
        }
        assert dependency != null;
        return block;
    }

    private Node requireDependency() {
        Node dependency = this.dependency;
        if (dependency == null) {
            assert currentBlock == null;
            throw noBlock();
        }
        assert currentBlock != null;
        return dependency;
    }

    private void checkReceiver(final Value targetPtr, final Value receiver) {
        if (receiver.getType() instanceof VoidType) {
            if (targetPtr.getPointeeType() instanceof InstanceMethodType) {
                getContext().error(getLocation(), "Call to instance method without receiver");
            }
        } else {
            if (targetPtr.getPointeeType() instanceof StaticMethodType) {
                getContext().error(getLocation(), "Call to static method with receiver");
            }
        }
    }

    private IllegalStateException noBlock() {
        return new IllegalStateException("No block in progress");
    }
}
