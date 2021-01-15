package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.CurrentThreadLiteral;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import io.smallrye.common.constraint.Assert;

final class SimpleBasicBlockBuilder implements BasicBlockBuilder, BasicBlockBuilder.ExceptionHandler {
    private final ExecutableElement element;
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
                mark(terminator.getSuccessor(i), from);
            }
        }
    }

    public ExceptionHandler getExceptionHandler() {
        if (policy == null) {
            return null;
        } else {
            ExceptionHandler handler = policy.computeCurrentExceptionHandler(this);
            return handler == this ? null : this;
        }
    }

    public Value add(final Value v1, final Value v2) {
        return new Add(line, bci, v1, v2);
    }

    public Value multiply(final Value v1, final Value v2) {
        return new Multiply(line, bci, v1, v2);
    }

    public Value and(final Value v1, final Value v2) {
        return new And(line, bci, v1, v2);
    }

    public Value or(final Value v1, final Value v2) {
        return new Or(line, bci, v1, v2);
    }

    public Value xor(final Value v1, final Value v2) {
        return new Xor(line, bci, v1, v2);
    }

    public Value cmpEq(final Value v1, final Value v2) {
        return new CmpEq(line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value cmpNe(final Value v1, final Value v2) {
        return new CmpNe(line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value shr(final Value v1, final Value v2) {
        return new Shr(line, bci, v1, v2);
    }

    public Value shl(final Value v1, final Value v2) {
        return new Shl(line, bci, v1, v2);
    }

    public Value sub(final Value v1, final Value v2) {
        return new Sub(line, bci, v1, v2);
    }

    public Value divide(final Value v1, final Value v2) {
        return new Div(line, bci, v1, v2);
    }

    public Value remainder(final Value v1, final Value v2) {
        return new Mod(line, bci, v1, v2);
    }

    public Value cmpLt(final Value v1, final Value v2) {
        return new CmpLt(line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value cmpGt(final Value v1, final Value v2) {
        return new CmpGt(line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value cmpLe(final Value v1, final Value v2) {
        return new CmpLe(line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value cmpGe(final Value v1, final Value v2) {
        return new CmpGe(line, bci, v1, v2, typeSystem.getBooleanType());
    }

    public Value rol(final Value v1, final Value v2) {
        return new Rol(line, bci, v1, v2);
    }

    public Value ror(final Value v1, final Value v2) {
        return new Ror(line, bci, v1, v2);
    }

    public Value negate(final Value v) {
        return new Neg(line, bci, v);
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

    public Value arrayLength(final Value array) {
        return new ArrayLength(line, bci, array, typeSystem.getSignedInteger32Type());
    }

    public Value truncate(final Value value, final WordType toType) {
        return new Truncate(line, bci, value, toType);
    }

    public Value extend(final Value value, final WordType toType) {
        return new Extend(line, bci, value, toType);
    }

    public Value bitCast(final Value value, final WordType toType) {
        return new BitCast(line, bci, value, toType);
    }

    public Value valueConvert(final Value value, final WordType toType) {
        return new Convert(line, bci, value, toType);
    }

    public Value instanceOf(final Value input, final ValueType expectedType) {
        return new InstanceOf(line, bci, input, expectedType, typeSystem.getBooleanType());
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        throw new IllegalStateException("InstanceOf of unresolved type");
    }

    public Value narrow(final Value value, final ValueType toType) {
        return new Narrow(line, bci, value, toType);
    }

    public Value narrow(final Value value, final TypeDescriptor desc) {
        throw new IllegalStateException("Narrow of unresolved type");
    }

    public Value memberPointer(final Value structPointer, final CompoundType.Member member) {
        return new MemberPointer(line, bci, structPointer, member);
    }

    public Value stackAllocate(final ValueType type, final Value count, final Value align) {
        return new StackAllocation(line, bci, type, count, align);
    }

    public Value receiver(final ObjectType upperBound) {
        return new ThisValue(Assert.checkNotNullParam("upperBound", upperBound).getReference());
    }

    public Value parameter(final ValueType type, final int index) {
        return new ParameterValue(type, index);
    }

    public PhiValue phi(final ValueType type, final BlockLabel owner) {
        return new PhiValue(line, bci, type, owner);
    }

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        return new Select(line, bci, condition, trueValue, falseValue);
    }

    public Value typeIdOf(final Value value) {
        return new TypeIdOf(line, bci, value);
    }

    public Value classOf(final Value typeId) {
        ClassObjectType type = element.getEnclosingType().getContext().findDefinedType("java/lang/Class").validate().getClassType();
        return new ClassOf(line, bci, typeId, type.getReference());
    }

    public Value new_(final ClassObjectType type) {
        return new New(line, bci, type);
    }

    public Value new_(final ClassTypeDescriptor desc) {
        throw new IllegalStateException("New of unresolved class");
    }

    public Value newArray(final ArrayObjectType arrayType, final Value size) {
        return asDependency(new NewArray(line, bci, requireDependency(), arrayType, size));
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        throw new IllegalStateException("New of unresolved array type");
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        return asDependency(new MultiNewArray(line, bci, requireDependency(), arrayType, dimensions));
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        throw new IllegalStateException("New of unresolved array type");
    }

    public Value clone(final Value object) {
        return asDependency(new Clone(line, bci, requireDependency(), object));
    }

    public Value pointerLoad(final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return asDependency(new PointerLoad(line, bci, requireDependency(), pointer, accessMode, atomicityMode));
    }

    public Value readInstanceField(final Value instance, final FieldElement fieldElement, final JavaAccessMode mode) {
        return asDependency(new InstanceFieldRead(line, bci, requireDependency(), instance, fieldElement, fieldElement.getType(List.of()), mode));
    }

    public Value readInstanceField(final Value instance, final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final JavaAccessMode mode) {
        throw new IllegalStateException("Access of unresolved field");
    }

    public Value readStaticField(final FieldElement fieldElement, final JavaAccessMode mode) {
        return asDependency(new StaticFieldRead(line, bci, requireDependency(), fieldElement, fieldElement.getType(List.of()), mode));
    }

    public Value readStaticField(final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final JavaAccessMode mode) {
        throw new IllegalStateException("Access of unresolved field");
    }

    public Value readArrayValue(final Value array, final Value index, final JavaAccessMode mode) {
        ValueType arrayType = array.getType();
        ValueType type;
        if (arrayType instanceof ReferenceType) {
            ArrayObjectType arrayTypeBound = (ArrayObjectType) ((ReferenceType) arrayType).getUpperBound();
            type = arrayTypeBound.getElementType();
            return asDependency(new ArrayElementRead(line, bci, requireDependency(), type, array, index, mode));
        } else if (arrayType instanceof ArrayType) {
            type = ((ArrayType) arrayType).getElementType();
            return asDependency(new ArrayElementRead(line, bci, requireDependency(), type, array, index, mode));
        } else {
            return asDependency(new ArrayElementRead(line, bci, requireDependency(), typeSystem.getPoisonType(), array, index, mode));
        }
    }

    public Node pointerStore(final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return asDependency(new PointerStore(line, bci, requireDependency(), pointer, value, accessMode, atomicityMode));
    }

    public Node writeInstanceField(final Value instance, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        return asDependency(new InstanceFieldWrite(line, bci, requireDependency(), instance, fieldElement, value, mode));
    }

    public Node writeInstanceField(final Value instance, final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final Value value, final JavaAccessMode mode) {
        throw new IllegalStateException("Access of unresolved field");
    }

    public Node writeStaticField(final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        return asDependency(new StaticFieldWrite(line, bci, requireDependency(), fieldElement, value, mode));
    }

    public Node writeStaticField(final TypeDescriptor owner, final String name, final TypeDescriptor descriptor, final Value value, final JavaAccessMode mode) {
        throw new IllegalStateException("Access of unresolved field");
    }

    public Node writeArrayValue(final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        return asDependency(new ArrayElementWrite(line, bci, requireDependency(), array, index, value, mode));
    }

    public Node fence(final MemoryAtomicityMode fenceType) {
        throw Assert.unsupported();
    }

    public Node monitorEnter(final Value obj) {
        return asDependency(new MonitorEnter(line, bci, requireDependency(), Assert.checkNotNullParam("obj", obj)));
    }

    public Node monitorExit(final Value obj) {
        return asDependency(new MonitorExit(line, bci, requireDependency(), Assert.checkNotNullParam("obj", obj)));
    }

    <N extends Node & Triable> N optionallyTry(N op) {
        ExceptionHandler exceptionHandler = getExceptionHandler();
        if (exceptionHandler != null) {
            BlockLabel resume = new BlockLabel();
            BlockLabel handler = exceptionHandler.getHandler();
            BlockLabel setupHandler = new BlockLabel();
            try_(op, resume, setupHandler);
            // this is the entry point for the stack unwinder
            begin(setupHandler);
            ClassContext classContext = element.getEnclosingType().getContext();
            CompilationContext ctxt = classContext.getCompilationContext();
            CurrentThreadLiteral thr = ctxt.getCurrentThreadValue();
            FieldElement exceptionField = ctxt.getExceptionField();
            Value exceptionValue = readInstanceField(thr, exceptionField, JavaAccessMode.PLAIN);
            writeInstanceField(thr, exceptionField, ctxt.getLiteralFactory().literalOfNull(), JavaAccessMode.PLAIN);
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
        return optionallyTry(new StaticInvocation(line, bci, requireDependency(), target, arguments));
    }

    public Node invokeStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        throw new IllegalStateException("Invoke of unresolved method");
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        return optionallyTry(new InstanceInvocation(line, bci, requireDependency(), kind, instance, target, arguments));
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        throw new IllegalStateException("Invoke of unresolved method");
    }

    public Node invokeDynamic(final MethodElement bootstrapMethod, final List<Value> staticArguments, final List<Value> arguments) {
        return optionallyTry(new DynamicInvocation(line, bci, requireDependency(), bootstrapMethod, staticArguments, arguments));
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        return optionallyTry(new StaticInvocationValue(line, bci, requireDependency(), target, target.getType(List.of()).getReturnType(), arguments));
    }

    public Value invokeValueStatic(final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        throw new IllegalStateException("Invoke of unresolved method");
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        return optionallyTry(new InstanceInvocationValue(line, bci, requireDependency(), kind, instance, target, target.getType(List.of()).getReturnType(), arguments));
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final TypeDescriptor owner, final String name, final MethodDescriptor descriptor, final List<Value> arguments) {
        throw new IllegalStateException("Invoke of unresolved method");
    }

    public Value invokeValueDynamic(final MethodElement bootstrapMethod, final List<Value> staticArguments, final ValueType type, final List<Value> arguments) {
        return optionallyTry(new DynamicInvocationValue(line, bci, requireDependency(), bootstrapMethod, staticArguments, type, arguments));
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        return optionallyTry(new ConstructorInvocation(line, bci, requireDependency(), instance, target, arguments));
    }

    public Value invokeConstructor(final Value instance, final TypeDescriptor owner, final MethodDescriptor descriptor, final List<Value> arguments) {
        throw new IllegalStateException("Invoke of unresolved constructor");
    }

    public Value callFunction(final Value callTarget, final List<Value> arguments) {
        return optionallyTry(new FunctionCall(line, bci, requireDependency(), callTarget, arguments));
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
        return dependency = blockEntry = new BlockEntry(blockLabel);
    }

    public BasicBlock goto_(final BlockLabel resumeLabel) {
        return terminate(requireCurrentBlock(), new Goto(line, bci, blockEntry, dependency, resumeLabel));
    }

    public BasicBlock if_(final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget) {
        return terminate(requireCurrentBlock(), new If(line, bci, blockEntry, dependency, condition, trueTarget, falseTarget));
    }

    public BasicBlock return_() {
        return terminate(requireCurrentBlock(), new Return(line, bci, blockEntry, dependency));
    }

    public BasicBlock return_(final Value value) {
        return terminate(requireCurrentBlock(), new ValueReturn(line, bci, blockEntry, dependency, value));
    }

    public BasicBlock throw_(final Value value) {
        return terminate(requireCurrentBlock(), new Throw(line, bci, blockEntry, dependency, value));
    }

    public BasicBlock jsr(final BlockLabel subLabel, final BlockLiteral returnAddress) {
        return terminate(requireCurrentBlock(), new Jsr(line, bci, blockEntry, dependency, subLabel, returnAddress));
    }

    public BasicBlock ret(final Value address) {
        return terminate(requireCurrentBlock(), new Ret(line, bci, blockEntry, dependency, address));
    }

    public BasicBlock try_(final Triable operation, final BlockLabel resumeLabel, final BlockLabel exceptionHandler) {
        return terminate(requireCurrentBlock(), new Try(operation, blockEntry, resumeLabel, exceptionHandler));
    }

    public BasicBlock classCastException(final Value fromType, final Value toType) {
        return terminate(requireCurrentBlock(), new ClassCastErrorNode(line, bci, blockEntry, dependency, fromType, toType));
    }

    public BasicBlock noSuchMethodError(final ObjectType owner, final MethodDescriptor desc, final String name) {
        return terminate(requireCurrentBlock(), new NoSuchMethodErrorNode(line, bci, blockEntry, dependency, owner, desc, name));
    }

    public BasicBlock classNotFoundError(final String name) {
        return terminate(requireCurrentBlock(), new ClassNotFoundErrorNode(line, bci, blockEntry, dependency, name));
    }

    public BlockEntry getBlockEntry() {
        requireCurrentBlock();
        return blockEntry;
    }

    public BasicBlock switch_(final Value value, final int[] checkValues, final BlockLabel[] targets, final BlockLabel defaultTarget) {
        return terminate(requireCurrentBlock(), new Switch(line, bci, blockEntry, dependency, defaultTarget, checkValues, targets, value));
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
