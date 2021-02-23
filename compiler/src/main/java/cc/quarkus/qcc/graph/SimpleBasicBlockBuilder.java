package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.GlobalVariableElement;
import cc.quarkus.qcc.type.definition.element.LocalVariableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
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
    private Node callSite;

    SimpleBasicBlockBuilder(final ExecutableElement element, final TypeSystem typeSystem) {
        this.element = element;
        this.typeSystem = typeSystem;
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

    public void finish() {
        if (currentBlock != null) {
            throw new IllegalStateException("Current block not terminated");
        }
        if (firstBlock != null) {
            mark(BlockLabel.getTargetOf(firstBlock), null);
        }
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
        return new Cmp(callSite, element, line, bci, v1, v2);
    }

    public Value cmpG(Value v1, Value v2) {
        return new CmpG(callSite, element, line, bci, v1, v2);
    }

    public Value cmpL(Value v1, Value v2) {
        return new CmpL(callSite, element, line, bci, v1, v2);
    }

    public Value negate(final Value v) {
        return new Neg(callSite, element, line, bci, v);
    }

    public Value byteSwap(final Value v) {
        throw Assert.unsupported();
    }

    public Value bitReverse(final Value v) {
        throw Assert.unsupported();
    }

    public Value countLeadingZeros(final Value v) {
        throw Assert.unsupported();
    }

    public Value countTrailingZeros(final Value v) {
        throw Assert.unsupported();
    }

    public Value populationCount(final Value v) {
        throw Assert.unsupported();
    }

    public Value arrayLength(final ValueHandle arrayHandle) {
        return new ArrayLength(callSite, element, line, bci, arrayHandle, typeSystem.getSignedInteger32Type());
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

    public Value instanceOf(final Value input, final ValueType expectedType) {
        return new InstanceOf(callSite, element, line, bci, input, expectedType, typeSystem.getBooleanType());
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        throw new IllegalStateException("InstanceOf of unresolved type");
    }

    public Value narrow(final Value value, final ValueType toType) {
        return new Narrow(callSite, element, line, bci, value, toType);
    }

    public Value narrow(final Value value, final TypeDescriptor desc) {
        throw new IllegalStateException("Narrow of unresolved type");
    }

    public ValueHandle memberOf(final ValueHandle structHandle, final CompoundType.Member member) {
        return new MemberOf(callSite, element, line, bci, structHandle, member);
    }

    public ValueHandle elementOf(ValueHandle array, Value index) {
        return new ElementOf(callSite, element, line, bci, array, index);
    }

    public ValueHandle pointerHandle(Value pointer) {
        return new PointerHandle(callSite, element, line, bci, pointer);
    }

    public ValueHandle referenceHandle(Value reference) {
        return new ReferenceHandle(callSite, element, line, bci, reference);
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field) {
        return new InstanceFieldOf(element, line, bci, field, field.getType(List.of()), instance);
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Instance field of unresolved type");
    }

    public ValueHandle staticField(FieldElement field) {
        return new StaticField(element, line, bci, field, field.getType(List.of()));
    }

    public ValueHandle staticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        throw new IllegalStateException("Static field of unresolved type");
    }

    public ValueHandle globalVariable(GlobalVariableElement variable) {
        return new GlobalVariable(element, line, bci, variable, variable.getType(List.of()));
    }

    public ValueHandle localVariable(LocalVariableElement variable) {
        return new LocalVariable(element, line, bci, variable, variable.getType(List.of()));
    }

    public Value addressOf(ValueHandle handle) {
        return new AddressOf(callSite, element, line, bci, handle);
    }

    public Value stackAllocate(final ValueType type, final Value count, final Value align) {
        return new StackAllocation(callSite, element, line, bci, type, count, align);
    }

    public ParameterValue parameter(final ValueType type, String label, final int index) {
        return new ParameterValue(callSite, element, type, label, index);
    }

    public Value currentThread() {
        ClassObjectType type = element.getEnclosingType().getContext().findDefinedType("java/lang/Thread").validate().getClassType();
        return new CurrentThreadRead(callSite, element, line, bci, requireDependency(), type.getReference());
    }

    public PhiValue phi(final ValueType type, final BlockLabel owner) {
        return new PhiValue(callSite, element, line, bci, type, owner);
    }

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        return new Select(callSite, element, line, bci, condition, trueValue, falseValue);
    }

    public Value typeIdOf(final ValueHandle valueHandle) {
        return new TypeIdOf(callSite, element, line, bci, valueHandle);
    }

    public Value classOf(final Value typeId) {
        ClassObjectType type = element.getEnclosingType().getContext().findDefinedType("java/lang/Class").validate().getClassType();
        return new ClassOf(callSite, element, line, bci, typeId, type.getReference());
    }

    public Value new_(final ClassObjectType type) {
        return new New(callSite, element, line, bci, type);
    }

    public Value new_(final ClassTypeDescriptor desc) {
        throw new IllegalStateException("New of unresolved class");
    }

    public Value newArray(final ArrayObjectType arrayType, final Value size) {
        return new NewArray(callSite, element, line, bci, arrayType, size);
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        throw new IllegalStateException("New of unresolved array type");
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        return new MultiNewArray(callSite, element, line, bci, null, arrayType, dimensions);
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

    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode) {
        CompilationContext ctxt = getCurrentElement().getEnclosingType().getContext().getCompilationContext();
        return asDependency(new CmpAndSwap(callSite, element, line, bci, CmpAndSwap.getResultType(ctxt, target.getValueType()), requireDependency(), target, expect, update, successMode, failureMode));
    }

    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        return asDependency(new Store(callSite, element, line, bci, requireDependency(), handle, value, mode));
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

    <N extends Node & Triable> N optionallyTry(N op) {
        ExceptionHandler exceptionHandler = getExceptionHandler();
        // todo: temporarily disable until exception handlers are fixed
        if (false && exceptionHandler != null) {
            BlockLabel resume = new BlockLabel();
            BlockLabel handler = exceptionHandler.getHandler();
            BlockLabel setupHandler = new BlockLabel();
            try_(op, resume, setupHandler);
            // this is the entry point for the stack unwinder
            begin(setupHandler);
            ClassContext classContext = element.getEnclosingType().getContext();
            CompilationContext ctxt = classContext.getCompilationContext();
            Value thr = getFirstBuilder().currentThread();
            FieldElement exceptionField = ctxt.getExceptionField();
            ValueHandle handle = instanceFieldOf(referenceHandle(thr), exceptionField);
            Value exceptionValue = load(handle, MemoryAtomicityMode.NONE);
            store(handle, ctxt.getLiteralFactory().literalOfNull(), MemoryAtomicityMode.NONE);
            BasicBlock sourceBlock = goto_(handler);
            exceptionHandler.enterHandler(sourceBlock, exceptionValue);
            begin(resume);
            return op;
        } else {
            return asDependency(op);
        }
    }

    public BlockLabel getHandler() {
        PhiValue exceptionPhi = this.exceptionPhi;
        if (exceptionPhi == null) {
            // first time called
            ClassObjectType typeId = getCurrentElement().getEnclosingType().getContext().findDefinedType("java/lang/Throwable").validate().getClassType();
            exceptionPhi = this.exceptionPhi = phi(typeId.getReference(), new BlockLabel());
        }
        return exceptionPhi.getPinnedBlockLabel();
    }

    public void enterHandler(final BasicBlock from, final Value exceptionValue) {
        exceptionPhi.setValueForBlock(element.getEnclosingType().getContext().getCompilationContext(), element, from, exceptionValue);
        BlockLabel handler = getHandler();
        if (! handler.hasTarget()) {
            begin(handler);
            throw_(exceptionPhi);
        }
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        return optionallyTry(new StaticInvocation(callSite, element, line, bci, requireDependency(), target, arguments));
    }

    public Node invokeStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        throw new IllegalStateException("Invoke of unresolved method: " + name);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        return optionallyTry(new InstanceInvocation(callSite, element, line, bci, requireDependency(), kind, instance, target, arguments));
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        throw new IllegalStateException("Invoke of unresolved method: " + name);
    }

    public Node invokeDynamic(final MethodElement bootstrapMethod, final List<Value> staticArguments, final List<Value> arguments) {
        return optionallyTry(new DynamicInvocation(callSite, element, line, bci, requireDependency(), bootstrapMethod, staticArguments, arguments));
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        return optionallyTry(new StaticInvocationValue(callSite, element, line, bci, requireDependency(), target, target.getType(List.of()).getReturnType(), arguments));
    }

    public Value invokeValueStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        throw new IllegalStateException("Invoke of unresolved method: " + name);
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        return optionallyTry(new InstanceInvocationValue(callSite, element, line, bci, requireDependency(), kind, instance, target, target.getType(List.of()).getReturnType(), arguments));
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        throw new IllegalStateException("Invoke of unresolved method: " + name);
    }

    public Value invokeValueDynamic(final MethodElement bootstrapMethod, final List<Value> staticArguments, final ValueType type, final List<Value> arguments) {
        return optionallyTry(new DynamicInvocationValue(callSite, element, line, bci, requireDependency(), bootstrapMethod, staticArguments, type, arguments));
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        return optionallyTry(new ConstructorInvocation(callSite, element, line, bci, requireDependency(), instance, target, arguments));
    }

    public Value invokeConstructor(final Value instance, final TypeDescriptor owner, final MethodDescriptor descriptor, final List<Value> arguments) {
        throw new IllegalStateException("Invoke of unresolved constructor");
    }

    public Value callFunction(final Value callTarget, final List<Value> arguments) {
        return optionallyTry(new FunctionCall(callSite, element, line, bci, requireDependency(), callTarget, arguments));
    }

    public Node nop() {
        return requireDependency();
    }

    private <N extends Node> N asDependency(N node) {
        this.dependency = node;
        return node;
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

    public BasicBlock try_(final Triable operation, final BlockLabel resumeLabel, final BlockLabel exceptionHandler) {
        return terminate(requireCurrentBlock(), new Try(callSite, element, operation, blockEntry, resumeLabel, exceptionHandler));
    }

    public BasicBlock classCastException(final Value fromType, final Value toType) {
        return terminate(requireCurrentBlock(), new ClassCastErrorNode(callSite, element, line, bci, blockEntry, dependency, fromType, toType));
    }

    public BasicBlock noSuchMethodError(final ObjectType owner, final MethodDescriptor desc, final String name) {
        return terminate(requireCurrentBlock(), new NoSuchMethodErrorNode(callSite, element, line, bci, blockEntry, dependency, owner, desc, name));
    }

    public BasicBlock classNotFoundError(final String name) {
        return terminate(requireCurrentBlock(), new ClassNotFoundErrorNode(callSite, element, line, bci, blockEntry, dependency, name));
    }

    public BlockEntry getBlockEntry() {
        requireCurrentBlock();
        return blockEntry;
    }

    public BasicBlock switch_(final Value value, final int[] checkValues, final BlockLabel[] targets, final BlockLabel defaultTarget) {
        return terminate(requireCurrentBlock(), new Switch(callSite, element, line, bci, blockEntry, dependency, defaultTarget, checkValues, targets, value));
    }

    private BasicBlock terminate(final BlockLabel block, final Terminator op) {
        BasicBlock realBlock = op.getTerminatedBlock();
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
