package org.qbicc.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.TypeType;
import org.qbicc.type.ValueType;
import org.qbicc.type.VoidType;
import org.qbicc.type.WordType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import io.smallrye.common.constraint.Assert;

final class SimpleBasicBlockBuilder implements BasicBlockBuilder, BasicBlockBuilder.ExceptionHandler {
    private final TypeSystem typeSystem;
    private BlockLabel firstBlock;
    private ExceptionHandlerPolicy policy;
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

    SimpleBasicBlockBuilder(final ExecutableElement element, final TypeSystem typeSystem) {
        this.element = element;
        this.typeSystem = typeSystem;
        this.rootElement = element;
        bci = - 1;
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

    public void setExceptionHandlerPolicy(final ExceptionHandlerPolicy policy) {
        this.policy = policy;
    }

    public void startMethod(List<ParameterValue> arguments) {
        started = true;
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

    public ExceptionHandler getExceptionHandler() {
        if (policy == null) {
            return null;
        } else {
            ExceptionHandler handler = policy.computeCurrentExceptionHandler(this);
            return handler == this ? null : handler;
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

    public Value arrayLength(final ValueHandle arrayHandle) {
        return asDependency(new ArrayLength(callSite, element, line, bci, requireDependency(), arrayHandle, typeSystem.getSignedInteger32Type()));
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
        return asDependency(new InstanceOf(callSite, element, line, bci, requireDependency(), input, expectedType, expectedDimensions, typeSystem.getBooleanType()));
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        throw new IllegalStateException("InstanceOf of unresolved type");
    }

    public Value checkcast(final Value value, final Value toType, final Value toDimensions, final CheckCast.CastType kind, final ObjectType expectedType) {
        ValueType inputType = value.getType();
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

    public Value deref(final Value value) {
        return new Deref(callSite, element, line, bci, value);
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

    public ValueHandle pointerHandle(Value pointer) {
        return new PointerHandle(callSite, element, line, bci, pointer);
    }

    public ValueHandle referenceHandle(Value reference) {
        return new ReferenceHandle(callSite, element, line, bci, reference);
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field) {
        return new InstanceFieldOf(element, line, bci, field, field.getType(), instance);
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Instance field of unresolved type");
    }

    public ValueHandle staticField(FieldElement field) {
        return new StaticField(element, line, bci, field, field.getType());
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

    public ValueHandle exactMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return new ExactMethodElementHandle(element, line, bci, method, instance, callSiteDescriptor, callSiteType);
    }

    public ValueHandle exactMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("Unresolved instance method");
    }

    public ValueHandle virtualMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return new VirtualMethodElementHandle(element, line, bci, method, instance, callSiteDescriptor, callSiteType);
    }

    public ValueHandle virtualMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("Unresolved instance method");
    }

    public ValueHandle interfaceMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return new InterfaceMethodElementHandle(element, line, bci, method, instance, callSiteDescriptor, callSiteType);
    }

    public ValueHandle interfaceMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("Unresolved instance method");
    }

    public ValueHandle staticMethod(MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return new StaticMethodElementHandle(element, line, bci, method, callSiteDescriptor, callSiteType);
    }

    public ValueHandle staticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        throw new IllegalStateException("Unresolved static method");
    }

    public ValueHandle constructorOf(Value instance, ConstructorElement constructor, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return new ConstructorElementHandle(element, line, bci, constructor, instance, callSiteDescriptor, callSiteType);
    }

    public ValueHandle constructorOf(Value instance, TypeDescriptor owner, MethodDescriptor descriptor) {
        throw new IllegalStateException("Unresolved constructor");
    }

    public ValueHandle functionOf(FunctionElement function) {
        return new FunctionElementHandle(element, line, bci, function);
    }

    public ValueHandle functionOf(Function function) {
        return new FunctionHandle(element, line, bci, function);
    }

    public ValueHandle functionOf(FunctionDeclaration function) {
        return new FunctionDeclarationHandle(element, line, bci, function);
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

    public Value currentThread() {
        ClassObjectType type = element.getEnclosingType().getContext().findDefinedType("java/lang/Thread").load().getClassType();
        return asDependency(new CurrentThreadRead(callSite, element, line, bci, requireDependency(), type.getReference()));
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

    public PhiValue phi(final ValueType type, final BlockLabel owner, PhiValue.Flag... flags) {
        boolean nullable = (flags.length == 0 || flags[0] != PhiValue.Flag.NOT_NULL);
        return new PhiValue(callSite, element, line, bci, type, owner, nullable);
    }

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        return new Select(callSite, element, line, bci, condition, trueValue, falseValue);
    }

    public Value typeIdOf(final ValueHandle valueHandle) {
        return new TypeIdOf(callSite, element, line, bci, valueHandle);
    }

    public Value classOf(Value typeId, Value dimensions) {
        Assert.assertTrue(typeId instanceof TypeLiteral);
        ClassContext classContext = element.getEnclosingType().getContext();
        ClassObjectType type = classContext.findDefinedType("java/lang/Class").load().getClassType();
        return new ClassOf(callSite, element, line, bci, typeId, dimensions, type.getReference());
    }

    public Value new_(final ClassObjectType type) {
        return asDependency(new New(callSite, element, line, bci, requireDependency(), type));
    }

    public Value new_(final ClassTypeDescriptor desc) {
        throw new IllegalStateException("New of unresolved class");
    }

    public Value newArray(final ArrayObjectType arrayType, final Value size) {
        return asDependency(new NewArray(callSite, element, line, bci, requireDependency(), arrayType, size));
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        throw new IllegalStateException("New of unresolved array type");
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        return asDependency(new MultiNewArray(callSite, element, line, bci, requireDependency(), arrayType, dimensions));
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        throw new IllegalStateException("New of unresolved array type");
    }

    public Value clone(final Value object) {
        return asDependency(new Clone(callSite, element, line, bci, requireDependency(), object));
    }

    public Value load(final ValueHandle handle, final MemoryAtomicityMode mode) {
        return asDependency(new Load(callSite, element, line, bci, requireDependency(), handle, mode));
    }

    public Value getAndAdd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return asDependency(new GetAndAdd(callSite, element, line, bci, requireDependency(), target, update, atomicityMode));
    }

    public Value getAndBitwiseAnd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return asDependency(new GetAndBitwiseAnd(callSite, element, line, bci, requireDependency(), target, update, atomicityMode));
    }

    public Value getAndBitwiseNand(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return asDependency(new GetAndBitwiseNand(callSite, element, line, bci, requireDependency(), target, update, atomicityMode));
    }

    public Value getAndBitwiseOr(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return asDependency(new GetAndBitwiseOr(callSite, element, line, bci, requireDependency(), target, update, atomicityMode));
    }

    public Value getAndBitwiseXor(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return asDependency(new GetAndBitwiseXor(callSite, element, line, bci, requireDependency(), target, update, atomicityMode));
    }

    public Value getAndSet(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return asDependency(new GetAndSet(callSite, element, line, bci, requireDependency(), target, update, atomicityMode));
    }

    public Value getAndSetMax(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return asDependency(new GetAndSetMax(callSite, element, line, bci, requireDependency(), target, update, atomicityMode));
    }

    public Value getAndSetMin(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return asDependency(new GetAndSetMin(callSite, element, line, bci, requireDependency(), target, update, atomicityMode));
    }

    public Value getAndSub(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return asDependency(new GetAndSub(callSite, element, line, bci, requireDependency(), target, update, atomicityMode));
    }

    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode, CmpAndSwap.Strength strength) {
        CompilationContext ctxt = getCurrentElement().getEnclosingType().getContext().getCompilationContext();
        return asDependency(new CmpAndSwap(callSite, element, line, bci, CmpAndSwap.getResultType(ctxt, target.getValueType()), requireDependency(), target, expect, update, successMode, failureMode, strength));
    }

    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        return asDependency(new Store(callSite, element, line, bci, requireDependency(), handle, value, mode));
    }

    public Node classInitCheck(final ObjectType objectType) {
        return asDependency(new ClassInitCheck(callSite, element, line, bci, requireDependency(), objectType));
    }

    public Node fence(final MemoryAtomicityMode fenceType) {
        return asDependency(new Fence(callSite, element, line, bci, requireDependency(), fenceType));
    }

    public Node monitorEnter(final Value obj) {
        return asDependency(new MonitorEnter(callSite, element, line, bci, requireDependency(), Assert.checkNotNullParam("obj", obj)));
    }

    public Node monitorExit(final Value obj) {
        return asDependency(new MonitorExit(callSite, element, line, bci, requireDependency(), Assert.checkNotNullParam("obj", obj)));
    }

    public Value call(ValueHandle target, List<Value> arguments) {
        // todo: this should be done in a separate BBB
        ExceptionHandler exceptionHandler = getExceptionHandler();
        if (exceptionHandler != null) {
            // promote to invoke
            return promoteToInvoke(target, arguments, exceptionHandler);
        }
        return asDependency(new Call(callSite, element, line, bci, requireDependency(), target, arguments));
    }

    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        // todo: this should be done in a separate BBB
        ExceptionHandler exceptionHandler = getExceptionHandler();
        if (exceptionHandler != null) {
            // promote to invoke
            return promoteToInvoke(target, arguments, exceptionHandler);
        }
        return new CallNoSideEffects(callSite, element, line, bci, target, arguments);
    }

    private Value promoteToInvoke(final ValueHandle target, final List<Value> arguments, final ExceptionHandler exceptionHandler) {
        BlockLabel resume = new BlockLabel();
        BlockLabel setupHandler = new BlockLabel();
        BlockLabel currentBlock = requireCurrentBlock();
        Value result = invoke(target, arguments, setupHandler, resume);
        BasicBlock from;
        if (result instanceof Invoke.ReturnValue) {
            from = ((Invoke.ReturnValue) result).getInvoke().getTerminatedBlock();
        } else {
            // invoke was rewritten or transformed
            from = BlockLabel.getTargetOf(currentBlock);
        }
        // this is the entry point for the stack unwinder
        setUpHandler(exceptionHandler, setupHandler, from);
        begin(resume);
        return result;
    }

    private void setUpHandler(final ExceptionHandler exceptionHandler, final BlockLabel setupHandler, BasicBlock from) {
        begin(setupHandler);
        ClassContext classContext = element.getEnclosingType().getContext();
        CompilationContext ctxt = classContext.getCompilationContext();
        Value thr = getFirstBuilder().currentThread();
        FieldElement exceptionField = ctxt.getExceptionField();
        ValueHandle handle = instanceFieldOf(referenceHandle(getFirstBuilder().notNull(thr)), exceptionField);
        LiteralFactory lf = ctxt.getLiteralFactory();
        Value exceptionValue = notNull(getAndSet(handle, lf.zeroInitializerLiteralOfType(handle.getValueType()), MemoryAtomicityMode.NONE));
        BasicBlock sourceBlock = goto_(exceptionHandler.getHandler());
        exceptionHandler.enterHandler(from, sourceBlock, exceptionValue);
    }

    public BlockLabel getHandler() {
        PhiValue exceptionPhi = this.exceptionPhi;
        if (exceptionPhi == null) {
            // first time called
            ClassObjectType typeId = getCurrentElement().getEnclosingType().getContext().findDefinedType("java/lang/Throwable").load().getClassType();
            exceptionPhi = this.exceptionPhi = phi(typeId.getReference(), new BlockLabel(), PhiValue.Flag.NOT_NULL);
        }
        return exceptionPhi.getPinnedBlockLabel();
    }

    public void enterHandler(final BasicBlock from, BasicBlock landingPad, final Value exceptionValue) {
        exceptionPhi.setValueForBlock(element.getEnclosingType().getContext().getCompilationContext(), element, from, exceptionValue);
        BlockLabel handler = getHandler();
        if (! handler.hasTarget()) {
            begin(handler);
            throw_(exceptionPhi);
        }
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

    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        // todo: this should be done in a separate BBB
        ExceptionHandler exceptionHandler = getExceptionHandler();
        if (exceptionHandler != null) {
            // promote to invoke
            BlockLabel setupHandler = new BlockLabel();
            BasicBlock result = invokeNoReturn(target, arguments, setupHandler);
            // this is the entry point for the stack unwinder
            setUpHandler(exceptionHandler, setupHandler, result);
            return result;
        }
        return terminate(requireCurrentBlock(), new CallNoReturn(callSite, element, line, bci, blockEntry, dependency, target, arguments));
    }

    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        return terminate(requireCurrentBlock(), new InvokeNoReturn(callSite, element, line, bci, blockEntry, dependency, target, arguments, catchLabel));
    }

    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        // todo: this should be done in a separate BBB
        ExceptionHandler exceptionHandler = getExceptionHandler();
        if (exceptionHandler != null) {
            // promote to invoke
            BlockLabel setupHandler = new BlockLabel();
            BasicBlock result = tailInvoke(target, arguments, setupHandler);
            // this is the entry point for the stack unwinder
            setUpHandler(exceptionHandler, setupHandler, result);
            return result;
        }
        return terminate(requireCurrentBlock(), new TailCall(callSite, element, line, bci, blockEntry, dependency, target, arguments));
    }

    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        return terminate(requireCurrentBlock(), new TailInvoke(callSite, element, line, bci, blockEntry, dependency, target, arguments, catchLabel));
    }

    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        final BlockLabel currentBlock = requireCurrentBlock();
        Invoke invoke = new Invoke(callSite, element, line, bci, blockEntry, dependency, target, arguments, catchLabel, resumeLabel);
        terminate(currentBlock, invoke);
        return invoke.getReturnValue();
    }

    public BasicBlock goto_(final BlockLabel resumeLabel) {
        return terminate(requireCurrentBlock(), new Goto(callSite, element, line, bci, blockEntry, dependency, resumeLabel));
    }

    public BasicBlock if_(final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget) {
        return terminate(requireCurrentBlock(), new If(callSite, element, line, bci, blockEntry, dependency, condition, trueTarget, falseTarget));
    }

    public BasicBlock return_() {
        return terminate(requireCurrentBlock(), new Return(callSite, element, line, bci, blockEntry, dependency));
    }

    public BasicBlock return_(final Value value) {
        if (value == null || value.getType() instanceof VoidType) {
            return return_();
        }
        return terminate(requireCurrentBlock(), new ValueReturn(callSite, element, line, bci, blockEntry, dependency, value));
    }

    public BasicBlock unreachable() {
        return terminate(requireCurrentBlock(), new Unreachable(callSite, element, line, bci, blockEntry, dependency));
    }

    public BasicBlock throw_(final Value value) {
        return terminate(requireCurrentBlock(), new Throw(callSite, element, line, bci, blockEntry, dependency, value));
    }

    public BasicBlock jsr(final BlockLabel subLabel, final BlockLiteral returnAddress) {
        return terminate(requireCurrentBlock(), new Jsr(callSite, element, line, bci, blockEntry, dependency, subLabel, returnAddress));
    }

    public BasicBlock ret(final Value address) {
        return terminate(requireCurrentBlock(), new Ret(callSite, element, line, bci, blockEntry, dependency, address));
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

    public BasicBlock switch_(final Value value, final int[] checkValues, final BlockLabel[] targets, final BlockLabel defaultTarget) {
        return terminate(requireCurrentBlock(), new Switch(callSite, element, line, bci, blockEntry, dependency, defaultTarget, checkValues, targets, value));
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

    private IllegalStateException noBlock() {
        return new IllegalStateException("No block in progress");
    }
}
