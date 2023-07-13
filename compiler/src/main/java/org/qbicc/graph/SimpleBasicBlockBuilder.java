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
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.ProgramObjectLiteral;
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
    private Node callSite;
    private BasicBlock terminatedBlock;
    private Map<BlockLabel, Map<Slot, BlockParameter>> parameters;
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
        parameter = new BlockParameter(callSite, element, line, bci, type, nullable, owner, slot);
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

    public ExecutableElement getCurrentElement() {
        return element;
    }

    public ExecutableElement getRootElement() { return rootElement; }

    public ExecutableElement setCurrentElement(final ExecutableElement element) {
        ExecutableElement old = this.element;
        this.element = element;
        return old;
    }

    public Node getCallSite() {
        return callSite;
    }

    public Node setCallSite(final Node callSite) {
        Node old = this.callSite;
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

    public int setBytecodeIndex(final int newBytecodeIndex) {
        try {
            return bci;
        } finally {
            bci = newBytecodeIndex;
        }
    }

    public int getBytecodeIndex() {
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
        return unique(new Add(callSite, element, line, bci, v1, v2));
    }

    public Value multiply(final Value v1, final Value v2) {
        return unique(new Multiply(callSite, element, line, bci, v1, v2));
    }

    public Value and(final Value v1, final Value v2) {
        return unique(new And(callSite, element, line, bci, v1, v2));
    }

    public Value or(final Value v1, final Value v2) {
        return unique(new Or(callSite, element, line, bci, v1, v2));
    }

    public Value xor(final Value v1, final Value v2) {
        return unique(new Xor(callSite, element, line, bci, v1, v2));
    }

    public Value isEq(final Value v1, final Value v2) {
        return unique(new IsEq(callSite, element, line, bci, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value isNe(final Value v1, final Value v2) {
        return unique(new IsNe(callSite, element, line, bci, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value shr(final Value v1, final Value v2) {
        return unique(new Shr(callSite, element, line, bci, v1, v2));
    }

    public Value shl(final Value v1, final Value v2) {
        return unique(new Shl(callSite, element, line, bci, v1, v2));
    }

    public Value sub(final Value v1, final Value v2) {
        return unique(new Sub(callSite, element, line, bci, v1, v2));
    }

    public Value divide(final Value v1, final Value v2) {
        return unique(new Div(callSite, element, line, bci, v1, v2, requireDependency()));
    }

    public Value remainder(final Value v1, final Value v2) {
        return unique(new Mod(callSite, element, line, bci, v1, v2, requireDependency()));
    }

    public Value min(final Value v1, final Value v2) {
        return unique(new Min(callSite, element, line, bci, v1, v2));
    }

    public Value max(final Value v1, final Value v2) {
        return unique(new Max(callSite, element, line, bci, v1, v2));
    }

    public Value isLt(final Value v1, final Value v2) {
        return unique(new IsLt(callSite, element, line, bci, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value isGt(final Value v1, final Value v2) {
        return unique(new IsGt(callSite, element, line, bci, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value isLe(final Value v1, final Value v2) {
        return unique(new IsLe(callSite, element, line, bci, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value isGe(final Value v1, final Value v2) {
        return unique(new IsGe(callSite, element, line, bci, v1, v2, getTypeSystem().getBooleanType()));
    }

    public Value rol(final Value v1, final Value v2) {
        return unique(new Rol(callSite, element, line, bci, v1, v2));
    }

    public Value ror(final Value v1, final Value v2) {
        return unique(new Ror(callSite, element, line, bci, v1, v2));
    }

    public Value cmp(Value v1, Value v2) {
        return unique(new Cmp(callSite, element, line, bci, v1, v2, getTypeSystem().getSignedInteger32Type()));
    }

    public Value cmpG(Value v1, Value v2) {
        return unique(new CmpG(callSite, element, line, bci, v1, v2, getTypeSystem().getSignedInteger32Type()));
    }

    public Value cmpL(Value v1, Value v2) {
        return unique(new CmpL(callSite, element, line, bci, v1, v2, getTypeSystem().getSignedInteger32Type()));
    }

    public Value notNull(Value v) {
        return v.isNullable() ? unique(new NotNull(callSite, element, line, bci, v)) : v;
    }

    public Value negate(final Value v) {
        return unique(new Neg(callSite, element, line, bci, v));
    }

    public Value complement(Value v) {
        Assert.checkNotNullParam("v", v);
        if (! (v.getType() instanceof IntegerType || v.getType() instanceof BooleanType)) {
            throw new IllegalArgumentException("Invalid input type");
        }
        return unique(new Comp(callSite, element, line, bci, v));
    }

    public Value byteSwap(final Value v) {
        return unique(new ByteSwap(callSite, element, line, bci, v));
    }

    public Value bitReverse(final Value v) {
        return unique(new BitReverse(callSite, element, line, bci, v));
    }

    public Value countLeadingZeros(final Value v) {
        return unique(new CountLeadingZeros(callSite, element, line, bci, v, getTypeSystem().getSignedInteger32Type()));
    }

    public Value countTrailingZeros(final Value v) {
        return unique(new CountTrailingZeros(callSite, element, line, bci, v, getTypeSystem().getSignedInteger32Type()));
    }

    public Value populationCount(final Value v) {
        return unique(new PopCount(callSite, element, line, bci, v, getTypeSystem().getSignedInteger32Type()));
    }

    public Value loadLength(final Value arrayPointer) {
        throw new IllegalStateException("loadLength not converted");
    }

    public Value loadTypeId(final Value objectPointer) {
        throw new IllegalStateException("loadTypeId not converted");
    }

    public Value truncate(final Value value, final WordType toType) {
        return unique(new Truncate(callSite, element, line, bci, value, toType));
    }

    public Value extend(final Value value, final WordType toType) {
        return unique(new Extend(callSite, element, line, bci, value, toType));
    }

    public Value bitCast(final Value value, final WordType toType) {
        return unique(new BitCast(callSite, element, line, bci, value, toType));
    }

    public Value fpToInt(final Value value, final IntegerType toType) {
        return unique(new FpToInt(callSite, element, line, bci, value, toType));
    }

    public Value intToFp(final Value value, final FloatType toType) {
        return unique(new IntToFp(callSite, element, line, bci, value, toType));
    }

    public Value decodeReference(Value refVal, PointerType pointerType) {
        // not asDependency() because the dependency may precede the required dependency
        return unique(new DecodeReference(callSite, element, line, bci, requireDependency(), refVal, pointerType));
    }

    public Value encodeReference(Value pointer, ReferenceType referenceType) {
        // not asDependency() because the dependency may precede the required dependency
        return unique(new EncodeReference(callSite, element, line, bci, requireDependency(), pointer, referenceType));
    }

    public Value instanceOf(final Value input, final ObjectType expectedType, final int expectedDimensions) {
        final BasicBlockBuilder fb = getFirstBuilder();
        ObjectType ifTrueExpectedType = expectedType;
        for (int i=0; i<expectedDimensions; i++) {
            ifTrueExpectedType = ifTrueExpectedType.getReferenceArrayObject();
        }
        return asDependency(new InstanceOf(callSite, element, line, bci, requireDependency(), input, fb.notNull(fb.bitCast(input, ((ReferenceType)input.getType()).narrow(ifTrueExpectedType))), expectedType, expectedDimensions, getTypeSystem().getBooleanType()));
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
        return asDependency(new CheckCast(callSite, element, line, bci, requireDependency(), value, toType, toDimensions, kind, expectedType));
    }

    public Value checkcast(final Value value, final TypeDescriptor desc) {
        throw new IllegalStateException("CheckCast of unresolved type");
    }

    public Value deref(Value pointer) {
        return unique(new Dereference(callSite, element, line, bci, pointer));
    }

    public Value invokeDynamic(MethodMethodHandleConstant bootstrapHandle, List<Literal> bootstrapArgs, String name, MethodDescriptor descriptor, List<Value> arguments) {
        getContext().error(getLocation(), "Unhandled `invokeDynamic`");
        throw new BlockEarlyTermination(unreachable());
    }

    public Value currentThread() {
        ReferenceType refType = element.getEnclosingType().getContext().getCompilationContext().getBootstrapClassContext().findDefinedType("java/lang/Thread").load().getObjectType().getReference();
        return unique(new CurrentThread(callSite, element, line, bci, refType));
    }

    public Value vaArg(Value vaList, ValueType type) {
        return asDependency(new VaArg(callSite, element, line, bci, requireDependency(), vaList, type));
    }

    public Value memberOf(final Value structPointer, final StructType.Member member) {
        return unique(new MemberOf(callSite, element, line, bci, structPointer, member));
    }

    public Value memberOfUnion(Value unionPointer, UnionType.Member member) {
        return unique(new MemberOfUnion(callSite, element, line, bci, unionPointer, member));
    }

    public Value elementOf(final Value arrayPointer, final Value index) {
        return unique(new ElementOf(callSite, element, line, bci, arrayPointer, index));
    }

    public Value offsetPointer(Value basePointer, Value offset) {
        return unique(new OffsetPointer(callSite, element, line, bci, basePointer, offset));
    }

    public Value pointerDifference(final Value leftPointer, final Value rightPointer) {
        return unique(new PointerDifference(callSite, element, line, bci, leftPointer, rightPointer));
    }

    public Value byteOffsetPointer(Value base, Value offset, ValueType outputType) {
        return unique(new ByteOffsetPointer(callSite, element, line, bci, base, offset, outputType));
    }

    @Override
    public Value lookupVirtualMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("lookupVirtualMethod of unresolved type");
    }

    @Override
    public Value lookupVirtualMethod(Value reference, InstanceMethodElement method) {
        return unique(new VirtualMethodLookup(callSite, element, line, bci, requireDependency(), reference, method));
    }

    @Override
    public Value lookupInterfaceMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("lookupInterfaceMethod of unresolved type");
    }

    @Override
    public Value lookupInterfaceMethod(Value reference, InstanceMethodElement method) {
        return unique(new InterfaceMethodLookup(callSite, element, line, bci, requireDependency(), reference, method));
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
        return unique(new ThreadBound(callSite, element, line, bci, threadPtr, target));
    }

    public Value resolveStaticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Static field of unresolved type");
    }

    public Value instanceFieldOf(Value instancePointer, InstanceFieldElement field) {
        return unique(new InstanceFieldOf(callSite, element, line, bci, instancePointer, field));
    }

    public Value instanceFieldOf(Value instancePointer, TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Instance field of unresolved type");
    }

    public Value auto(Value initializer) {
        return unique(new Auto(callSite, element, line, bci, requireDependency(), initializer));
    }

    public Value stackAllocate(final ValueType type, final Value count, final Value align) {
        return asDependency(new StackAllocation(callSite, element, line, bci, requireDependency(), type, count, align));
    }

    public Value offsetOfField(FieldElement fieldElement) {
        return unique(new OffsetOfField(callSite, element, line, bci, fieldElement, getTypeSystem().getSignedInteger32Type()));
    }

    public Value extractElement(Value array, Value index) {
        return unique(new ExtractElement(callSite, element, line, bci, array, index));
    }

    public Value extractMember(Value compound, StructType.Member member) {
        return unique(new ExtractMember(callSite, element, line, bci, compound, member));
    }

    public Value extractInstanceField(Value valueObj, TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Field access of unresolved class");
    }

    public Value extractInstanceField(Value valueObj, FieldElement field) {
        return new ExtractInstanceField(callSite, element, line, bci, valueObj, field, field.getType());
    }

    public Value insertElement(Value array, Value index, Value value) {
        return unique(new InsertElement(callSite, element, line, bci, array, index, value));
    }

    public Value insertMember(Value compound, StructType.Member member, Value value) {
        return unique(new InsertMember(callSite, element, line, bci, compound, value, member));
    }

    public Node declareDebugAddress(LocalVariableElement variable, Value address) {
        return asDependency(new DebugAddressDeclaration(callSite, element, line, bci, requireDependency(), variable, address));
    }

    public Node setDebugValue(LocalVariableElement variable, Value value) {
        return asDependency(new DebugValueDeclaration(callSite, element, line, bci, requireDependency(), variable, value));
    }

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        return unique(new Select(callSite, element, line, bci, condition, trueValue, falseValue));
    }

    public Value classOf(Value typeId, Value dimensions) {
        Assert.assertTrue(typeId instanceof TypeIdLiteral);
        ClassContext classContext = element.getEnclosingType().getContext();
        ClassObjectType type = classContext.findDefinedType("java/lang/Class").load().getClassType();
        return unique(new ClassOf(callSite, element, line, bci, typeId, dimensions, type.getReference()));
    }

    public Value new_(final ClassObjectType type, final Value typeId, final Value size, final Value align) {
        return asDependency(new New(callSite, element, line, bci, requireDependency(), type, typeId, size, align));
    }

    public Value new_(final ClassTypeDescriptor desc) {
        throw new IllegalStateException("New of unresolved class");
    }

    public Value newArray(final PrimitiveArrayObjectType arrayType, final Value size) {
        return asDependency(new NewArray(callSite, element, line, bci, requireDependency(), arrayType, size));
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        throw new IllegalStateException("New of unresolved array type");
    }

    public Value newReferenceArray(final ReferenceArrayObjectType arrayType, Value elemTypeId, Value dimensions, final Value size) {
        return asDependency(new NewReferenceArray(callSite, element, line, bci, requireDependency(), arrayType, elemTypeId, dimensions, size));
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        return asDependency(new MultiNewArray(callSite, element, line, bci, requireDependency(), arrayType, dimensions));
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        throw new IllegalStateException("New of unresolved array type");
    }

    public Value load(final Value pointer, final ReadAccessMode mode) {
        return asDependency(new Load(callSite, element, line, bci, requireDependency(), pointer, mode));
    }

    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return asDependency(new ReadModifyWrite(callSite, element, line, bci, requireDependency(), pointer, op, update, readMode, writeMode));
    }

    public Value cmpAndSwap(Value pointer, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        CompilationContext ctxt = getCurrentElement().getEnclosingType().getContext().getCompilationContext();
        return asDependency(new CmpAndSwap(callSite, element, line, bci, CmpAndSwap.getResultType(ctxt, pointer.getPointeeType()), requireDependency(), pointer, expect, update, readMode, writeMode, strength));
    }

    public Node store(Value handle, Value value, WriteAccessMode mode) {
        return asDependency(new Store(callSite, element, line, bci, requireDependency(), handle, value, mode));
    }

    public Node initCheck(InitializerElement initializer, Value initThunk) {
        return asDependency(new InitCheck(callSite, element, line, bci, requireDependency(), initializer, initThunk));
    }

    public Node initializeClass(final Value value) {
        return asDependency(new InitializeClass(callSite, element, line, bci, requireDependency(), value));
    }

    public Node fence(final GlobalAccessMode fenceType) {
        return asDependency(new Fence(callSite, element, line, bci, requireDependency(), fenceType));
    }

    public Node monitorEnter(final Value obj) {
        return asDependency(new MonitorEnter(callSite, element, line, bci, requireDependency(), Assert.checkNotNullParam("obj", obj)));
    }

    public Node monitorExit(final Value obj) {
        return asDependency(new MonitorExit(callSite, element, line, bci, requireDependency(), Assert.checkNotNullParam("obj", obj)));
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
        return asDependency(new Call(callSite, element, line, bci, requireDependency(), targetPtr, receiver, arguments));
    }

    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        checkReceiver(targetPtr, receiver);
        return unique(new CallNoSideEffects(callSite, element, line, bci, targetPtr, receiver, arguments));
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
        return dependency = blockEntry = new BlockEntry(callSite, element, blockLabel);
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
        final Node oldCallSite = callSite;
        final Map<BlockLabel, Map<Slot, BlockParameter>> oldParameters = new HashMap<>(parameters);
        try {
            return doBegin(blockLabel, arg, maker);
        } finally {
            // restore all state
            parameters = oldParameters;
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
            dependency = blockEntry = new BlockEntry(callSite, element, blockLabel);
            parameters = new HashMap<>();
            maker.accept(arg, firstBuilder);
            if (currentBlock != null) {
                getContext().error(getLocation(), "Block not terminated");
                firstBuilder.unreachable();
            }
        } catch (BlockEarlyTermination ignored) {
        }
        return BlockLabel.getTargetOf(blockLabel);
    }

    public Node reachable(final Value value) {
        return asDependency(new Reachable(callSite, element, line, bci, requireDependency(), value));
    }

    public Node safePoint() {
        return asDependency(new SafePoint(callSite, element, line, bci, requireDependency()));
    }

    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        checkReceiver(targetPtr, receiver);
        return terminate(requireCurrentBlock(), new CallNoReturn(callSite, element, line, bci, blockEntry, dependency, targetPtr, receiver, arguments));
    }

    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        checkReceiver(targetPtr, receiver);
        return terminate(requireCurrentBlock(), new InvokeNoReturn(callSite, element, line, bci, blockEntry, dependency, targetPtr, receiver, arguments, catchLabel, targetArguments));
    }

    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        checkReceiver(targetPtr, receiver);
        return terminate(requireCurrentBlock(), new TailCall(callSite, element, line, bci, blockEntry, dependency, targetPtr, receiver, arguments));
    }

    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        checkReceiver(targetPtr, receiver);
        final BlockLabel currentBlock = requireCurrentBlock();
        Invoke invoke = new Invoke(callSite, element, line, bci, blockEntry, dependency, targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
        terminate(currentBlock, invoke);
        // todo: addParam(resumeLabel, Slot.result(), targetPtr.getReturnType())
        return invoke.getReturnValue();
    }

    public BasicBlock goto_(final BlockLabel resumeLabel, final Map<Slot, Value> targetArguments) {
        return terminate(requireCurrentBlock(), new Goto(callSite, element, line, bci, blockEntry, dependency, resumeLabel, targetArguments));
    }

    public BasicBlock if_(final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget, final Map<Slot, Value> targetArguments) {
        return terminate(requireCurrentBlock(), new If(callSite, element, line, bci, blockEntry, dependency, condition, trueTarget, falseTarget, targetArguments));
    }

    public BasicBlock return_(final Value value) {
        if (value == null) {
            return return_(emptyVoid());
        }
        return terminate(requireCurrentBlock(), new Return(callSite, element, line, bci, blockEntry, dependency, value));
    }

    public BasicBlock unreachable() {
        return terminate(requireCurrentBlock(), new Unreachable(callSite, element, line, bci, blockEntry, dependency));
    }

    public BasicBlock throw_(final Value value) {
        return terminate(requireCurrentBlock(), new Throw(callSite, element, line, bci, blockEntry, dependency, value));
    }

    public BasicBlock ret(final Value address, Map<Slot, Value> targetArguments) {
        return terminate(requireCurrentBlock(), new Ret(callSite, element, line, bci, blockEntry, dependency, address, targetArguments));
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
        return terminate(requireCurrentBlock(), new Switch(callSite, element, line, bci, blockEntry, dependency, defaultTarget, checkValues, targets, value, targetArguments));
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
