package org.qbicc.graph;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import io.smallrye.common.constraint.Assert;
import org.eclipse.collections.api.factory.SortedMaps;
import org.eclipse.collections.api.map.sorted.ImmutableSortedMap;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.NullableType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

final class SimpleBasicBlockBuilder implements BasicBlockBuilder {
    private final TypeSystem typeSystem;
    private BlockLabel firstBlock;
    private int line;
    private int bci;
    private Node dependency;
    private BlockEntry blockEntry;
    private BlockLabel currentBlock;
    private PhiValue exceptionPhi;
    private BasicBlockBuilder firstBuilder;
    private ExecutableElement element;
    private final ExecutableElement rootElement;
    private Node callSite;
    private BasicBlock terminatedBlock;
    private boolean started;
    private ImmutableSortedMap<Slot, BlockParameter> parameters;

    SimpleBasicBlockBuilder(final ExecutableElement element, final TypeSystem typeSystem) {
        this.element = element;
        this.typeSystem = typeSystem;
        this.rootElement = element;
        bci = - 1;
        parameters = SortedMaps.immutable.empty();
    }

    @Override
    public BlockParameter addParam(Slot slot, ValueType type, boolean nullable) {
        BlockParameter parameter = parameters.get(slot);
        if (parameter != null) {
            throw new IllegalArgumentException("Parameter " + slot + " already defined");
        }
        if (nullable && ! (type instanceof NullableType)) {
            throw new IllegalArgumentException("Parameter can only be nullable if its type is nullable");
        }
        parameter = new BlockParameter(callSite, element, type, nullable, currentBlock, slot);
        parameters = parameters.newWithKeyValue(slot, parameter);
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

    public void startMethod(List<ParameterValue> arguments) {
        started = true;
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
        return new Add(callSite, element, line, bci, v1, v2);
    }

    public Value multiply(final Value v1, final Value v2) {
        return new Multiply(callSite, element, line, bci, v1, v2);
    }

    public Value and(final Value v1, final Value v2) {
        return new And(callSite, element, line, bci, v1, v2);
    }

    public Value or(final Value v1, final Value v2) {
        return new Or(callSite, element, line, bci, v1, v2);
    }

    public Value xor(final Value v1, final Value v2) {
        return new Xor(callSite, element, line, bci, v1, v2);
    }

    public Value isEq(final Value v1, final Value v2) {
        return new IsEq(callSite, element, line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value isNe(final Value v1, final Value v2) {
        return new IsNe(callSite, element, line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value shr(final Value v1, final Value v2) {
        return new Shr(callSite, element, line, bci, v1, v2);
    }

    public Value shl(final Value v1, final Value v2) {
        return new Shl(callSite, element, line, bci, v1, v2);
    }

    public Value sub(final Value v1, final Value v2) {
        return new Sub(callSite, element, line, bci, v1, v2);
    }

    public Value divide(final Value v1, final Value v2) {
        return new Div(callSite, element, line, bci, v1, v2);
    }

    public Value remainder(final Value v1, final Value v2) {
        return new Mod(callSite, element, line, bci, v1, v2);
    }

    public Value min(final Value v1, final Value v2) {
        return new Min(callSite, element, line, bci, v1, v2);
    }

    public Value max(final Value v1, final Value v2) {
        return new Max(callSite, element, line, bci, v1, v2);
    }

    public Value isLt(final Value v1, final Value v2) {
        return new IsLt(callSite, element, line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value isGt(final Value v1, final Value v2) {
        return new IsGt(callSite, element, line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value isLe(final Value v1, final Value v2) {
        return new IsLe(callSite, element, line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value isGe(final Value v1, final Value v2) {
        return new IsGe(callSite, element, line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value rol(final Value v1, final Value v2) {
        return new Rol(callSite, element, line, bci, v1, v2);
    }

    public Value ror(final Value v1, final Value v2) {
        return new Ror(callSite, element, line, bci, v1, v2);
    }

    public Value cmp(Value v1, Value v2) {
        return new Cmp(callSite, element, line, bci, v1, v2, typeSystem.getSignedInteger32Type());
    }

    public Value cmpG(Value v1, Value v2) {
        return new CmpG(callSite, element, line, bci, v1, v2, typeSystem.getSignedInteger32Type());
    }

    public Value cmpL(Value v1, Value v2) {
        return new CmpL(callSite, element, line, bci, v1, v2, typeSystem.getSignedInteger32Type());
    }

    public Value notNull(Value v) {
        return v.isNullable() ? new NotNull(callSite, element, line, bci, v) : v;
    }

    public Value negate(final Value v) {
        return new Neg(callSite, element, line, bci, v);
    }

    public Value complement(Value v) {
        Assert.checkNotNullParam("v", v);
        if (! (v.getType() instanceof IntegerType || v.getType() instanceof BooleanType)) {
            throw new IllegalArgumentException("Invalid input type");
        }
        return new Comp(callSite, element, line, bci, v);
    }

    public Value byteSwap(final Value v) {
        return new ByteSwap(callSite, element, line, bci, v);
    }

    public Value bitReverse(final Value v) {
        return new BitReverse(callSite, element, line, bci, v);
    }

    public Value countLeadingZeros(final Value v) {
        return new CountLeadingZeros(callSite, element, line, bci, v, typeSystem.getSignedInteger32Type());
    }

    public Value countTrailingZeros(final Value v) {
        return new CountTrailingZeros(callSite, element, line, bci, v, typeSystem.getSignedInteger32Type());
    }

    public Value populationCount(final Value v) {
        throw Assert.unsupported();
    }

    public ValueHandle lengthOf(final ValueHandle arrayHandle) {
        throw new IllegalStateException("lengthOf not converted");
    }

    public Value truncate(final Value value, final WordType toType) {
        return new Truncate(callSite, element, line, bci, value, toType);
    }

    public Value extend(final Value value, final WordType toType) {
        return new Extend(callSite, element, line, bci, value, toType);
    }

    public Value bitCast(final Value value, final WordType toType) {
        return new BitCast(callSite, element, line, bci, value, toType);
    }

    public Value valueConvert(final Value value, final WordType toType) {
        return new Convert(callSite, element, line, bci, value, toType);
    }

    public Value instanceOf(final Value input, final ObjectType expectedType, final int expectedDimensions) {
        final BasicBlockBuilder fb = getFirstBuilder();
        ObjectType ifTrueExpectedType = expectedType;
        for (int i=0; i<expectedDimensions; i++) {
            ifTrueExpectedType = ifTrueExpectedType.getReferenceArrayObject();
        }
        return asDependency(new InstanceOf(callSite, element, line, bci, requireDependency(), input, fb.notNull(fb.bitCast(input, ((ReferenceType)input.getType()).narrow(ifTrueExpectedType))), expectedType, expectedDimensions, typeSystem.getBooleanType()));
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
            throw new IllegalArgumentException("Only references can be checkcast");
        }
        ValueType toTypeTypeRaw = toType.getType();
        if (! (toTypeTypeRaw instanceof TypeType)) {
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

    public Value selectMember(ValueHandle handle) {
        TypeSystem ts = element.getEnclosingType().getContext().getTypeSystem();
        return new MemberSelector(callSite, element, line, bci, handle, ts.getVoidType());
    }

    public ValueHandle currentThread() {
        ReferenceType refType = element.getEnclosingType().getContext().getCompilationContext().getBootstrapClassContext().findDefinedType("java/lang/Thread").load().getObjectType().getReference();
        return new CurrentThread(element, line, bci, refType);
    }

    public Value vaArg(Value vaList, ValueType type) {
        return asDependency(new VaArg(callSite, element, line, bci, requireDependency(), vaList, type));
    }

    public ValueHandle memberOf(final ValueHandle structHandle, final CompoundType.Member member) {
        return new MemberOf(callSite, element, line, bci, structHandle, member);
    }

    public ValueHandle elementOf(ValueHandle array, Value index) {
        return new ElementOf(callSite, element, line, bci, array, index);
    }

    public ValueHandle unsafeHandle(ValueHandle base, Value offset, ValueType outputType) {
        return new UnsafeHandle(callSite, element, line, bci, base, offset, outputType);
    }

    public ValueHandle pointerHandle(Value pointer, Value offsetValue) {
        return new PointerHandle(callSite, element, line, bci, pointer, offsetValue);
    }

    public ValueHandle referenceHandle(Value reference) {
        return new ReferenceHandle(callSite, element, line, bci, reference);
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field) {
        return new InstanceFieldOf(element, line, bci, (InstanceFieldElement) field, field.getType(), instance);
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Instance field of unresolved type");
    }

    public ValueHandle staticField(FieldElement field) {
        return new StaticField(element, line, bci, (StaticFieldElement) field, field.getType());
    }

    public ValueHandle staticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Static field of unresolved type");
    }

    public ValueHandle globalVariable(GlobalVariableElement variable) {
        return new GlobalVariable(element, line, bci, variable, variable.getType());
    }

    public ValueHandle localVariable(LocalVariableElement variable) {
        return new LocalVariable(element, line, bci, variable, variable.getType());
    }

    public ValueHandle exactMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        return new ExactMethodElementHandle(element, line, bci, method, instance, callSiteDescriptor, callSiteType);
    }

    public ValueHandle exactMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("Unresolved instance method");
    }

    public ValueHandle virtualMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        return new VirtualMethodElementHandle(element, line, bci, method, instance, callSiteDescriptor, callSiteType);
    }

    public ValueHandle virtualMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("Unresolved instance method");
    }

    public ValueHandle interfaceMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        return new InterfaceMethodElementHandle(element, line, bci, method, instance, callSiteDescriptor, callSiteType);
    }

    public ValueHandle interfaceMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("Unresolved instance method");
    }

    public ValueHandle staticMethod(MethodElement method, MethodDescriptor callSiteDescriptor, StaticMethodType callSiteType) {
        return new StaticMethodElementHandle(element, line, bci, method, callSiteDescriptor, callSiteType);
    }

    public ValueHandle staticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("Unresolved static method");
    }

    public ValueHandle constructorOf(Value instance, ConstructorElement constructor, MethodDescriptor callSiteDescriptor, InstanceMethodType callSiteType) {
        return new ConstructorElementHandle(element, line, bci, constructor, instance, callSiteDescriptor, callSiteType);
    }

    public ValueHandle constructorOf(Value instance, TypeDescriptor owner, MethodDescriptor descriptor) {
        throw new IllegalStateException("Unresolved constructor");
    }

    public ValueHandle functionOf(FunctionElement function) {
        return new FunctionElementHandle(element, line, bci, function);
    }

    public ValueHandle asm(String instruction, String constraints, Set<AsmHandle.Flag> flags, FunctionType type) {
        return new AsmHandle(callSite, element, line, bci, instruction, constraints, flags, type);
    }

    public Value addressOf(ValueHandle handle) {
        return new AddressOf(callSite, element, line, bci, handle);
    }

    public Value referenceTo(ValueHandle handle) throws IllegalArgumentException {
        return new ReferenceTo(callSite, element, line, bci, handle);
    }

    public Value stackAllocate(final ValueType type, final Value count, final Value align) {
        return asDependency(new StackAllocation(callSite, element, line, bci, requireDependency(), type, count, align));
    }

    public ParameterValue parameter(final ValueType type, String label, final int index) {
        return new ParameterValue(callSite, element, type, label, index);
    }

    public Value offsetOfField(FieldElement fieldElement) {
        return new OffsetOfField(callSite, element, line, bci, fieldElement, typeSystem.getSignedInteger32Type());
    }

    public Value extractElement(Value array, Value index) {
        return new ExtractElement(callSite, element, line, bci, array, index);
    }

    public Value extractMember(Value compound, CompoundType.Member member) {
        return new ExtractMember(callSite, element, line, bci, compound, member);
    }

    public Value extractInstanceField(Value valueObj, TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Field access of unresolved class");
    }

    public Value extractInstanceField(Value valueObj, FieldElement field) {
        return new ExtractInstanceField(callSite, element, line, bci, valueObj, field, field.getType());
    }

    public Value insertElement(Value array, Value index, Value value) {
        return new InsertElement(callSite, element, line, bci, array, index, value);
    }

    public Value insertMember(Value compound, CompoundType.Member member, Value value) {
        return new InsertMember(callSite, element, line, bci, compound, value, member);
    }

    public Node declareDebugAddress(LocalVariableElement variable, Value address) {
        return asDependency(new DebugAddressDeclaration(callSite, element, line, bci, requireDependency(), variable, address));
    }

    public Node setDebugValue(LocalVariableElement variable, Value value) {
        return asDependency(new DebugValueDeclaration(callSite, element, line, bci, requireDependency(), variable, value));
    }

    public PhiValue phi(final ValueType type, final BlockLabel owner, PhiValue.Flag... flags) {
        boolean nullable = (flags.length == 0 || flags[0] != PhiValue.Flag.NOT_NULL);
        return new PhiValue(callSite, element, line, bci, type, owner, nullable);
    }

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        return new Select(callSite, element, line, bci, condition, trueValue, falseValue);
    }

    public Value classOf(Value typeId, Value dimensions) {
        Assert.assertTrue(typeId instanceof TypeLiteral);
        ClassContext classContext = element.getEnclosingType().getContext();
        ClassObjectType type = classContext.findDefinedType("java/lang/Class").load().getClassType();
        return new ClassOf(callSite, element, line, bci, typeId, dimensions, type.getReference());
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

    public Value load(final ValueHandle handle, final ReadAccessMode mode) {
        return asDependency(new Load(callSite, element, line, bci, requireDependency(), handle, mode));
    }

    public Value readModifyWrite(ValueHandle target, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return asDependency(new ReadModifyWrite(callSite, element, line, bci, requireDependency(), target, op, update, readMode, writeMode));
    }

    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        CompilationContext ctxt = getCurrentElement().getEnclosingType().getContext().getCompilationContext();
        return asDependency(new CmpAndSwap(callSite, element, line, bci, CmpAndSwap.getResultType(ctxt, target.getPointeeType()), requireDependency(), target, expect, update, readMode, writeMode, strength));
    }

    public Node store(ValueHandle handle, Value value, WriteAccessMode mode) {
        return asDependency(new Store(callSite, element, line, bci, requireDependency(), handle, value, mode));
    }

    public Node initCheck(InitializerElement initializer, Value initThunk) {
        return asDependency(new InitCheck(callSite, element, line, bci, requireDependency(), initializer, initThunk));
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

    public Value call(ValueHandle target, List<Value> arguments) {
        return asDependency(new Call(callSite, element, line, bci, requireDependency(), target, arguments));
    }

    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        return new CallNoSideEffects(callSite, element, line, bci, target, arguments);
    }

    public Node nop() {
        return requireDependency();
    }

    private <N extends Node> N asDependency(N node) {
        this.dependency = node;
        return node;
    }

    public Node begin(final BlockLabel blockLabel) {
        if (!started) {
            throw new IllegalStateException("begin() called before startMethod()");
        }
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
        if (!started) {
            throw new IllegalStateException("begin() called before startMethod()");
        }
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
        final ImmutableSortedMap<Slot, BlockParameter> oldParameters = parameters;
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
            parameters = SortedMaps.immutable.empty();
            maker.accept(arg, firstBuilder);
            if (currentBlock != null) {
                getContext().error(getLocation(), "Block not terminated");
                firstBuilder.unreachable();
            }
        } catch (BlockEarlyTermination bet) {
        }
        return BlockLabel.getTargetOf(blockLabel);
    }

    public Node reachable(final Value value) {
        return asDependency(new Reachable(callSite, element, line, bci, requireDependency(), value));
    }

    public Node safePoint() {
        return asDependency(new SafePoint(callSite, element, line, bci, requireDependency()));
    }

    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        return terminate(requireCurrentBlock(), new CallNoReturn(callSite, element, line, bci, blockEntry, dependency, target, arguments));
    }

    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return terminate(requireCurrentBlock(), new InvokeNoReturn(callSite, element, line, bci, blockEntry, dependency, target, arguments, catchLabel, targetArguments));
    }

    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        return terminate(requireCurrentBlock(), new TailCall(callSite, element, line, bci, blockEntry, dependency, target, arguments));
    }

    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return terminate(requireCurrentBlock(), new TailInvoke(callSite, element, line, bci, blockEntry, dependency, target, arguments, catchLabel, targetArguments));
    }

    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        final BlockLabel currentBlock = requireCurrentBlock();
        Invoke invoke = new Invoke(callSite, element, line, bci, blockEntry, dependency, target, arguments, catchLabel, resumeLabel, targetArguments);
        terminate(currentBlock, invoke);
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
        parameters = SortedMaps.immutable.empty();
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

    private IllegalStateException noBlock() {
        return new IllegalStateException("No block in progress");
    }
}
