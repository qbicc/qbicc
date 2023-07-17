package org.qbicc.graph;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

import org.qbicc.context.Location;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.Literal;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.StructType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.UnionType;
import org.qbicc.type.ValueType;
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

/**
 * A graph factory which delegates all operations to another graph factory.  Can be used as a base class for graph
 * modifying plugins.
 */
public class DelegatingBasicBlockBuilder implements BasicBlockBuilder {
    private final BasicBlockBuilder delegate;
    private final BasicBlockBuilder last;

    public DelegatingBasicBlockBuilder(final BasicBlockBuilder delegate) {
        this.delegate = delegate;
        if (delegate instanceof DelegatingBasicBlockBuilder) {
            last = ((DelegatingBasicBlockBuilder) delegate).last;
        } else {
            last = delegate;
        }
        setFirstBuilder(this);
    }

    public BlockParameter addParam(BlockLabel owner, Slot slot, ValueType type, boolean nullable) {
        return delegate.addParam(owner, slot, type, nullable);
    }

    public BlockParameter getParam(BlockLabel owner, Slot slot) throws NoSuchElementException {
        return delegate.getParam(owner, slot);
    }

    public BasicBlockBuilder getFirstBuilder() {
        return last.getFirstBuilder();
    }

    public void setFirstBuilder(final BasicBlockBuilder first) {
        last.setFirstBuilder(first);
    }

    public ExecutableElement element() {
        return getDelegate().element();
    }

    public ExecutableElement getRootElement() {
        return getDelegate().getRootElement();
    }

    public ExecutableElement setCurrentElement(final ExecutableElement element) {
        return getDelegate().setCurrentElement(element);
    }

    public Node callSite() {
        return getDelegate().callSite();
    }

    public Node setCallSite(final Node callSite) {
        return getDelegate().setCallSite(callSite);
    }

    public Location getLocation() {
        return getDelegate().getLocation();
    }

    public int lineNumber() {
        return getDelegate().lineNumber();
    }

    public int setLineNumber(final int newLineNumber) {
        return getDelegate().setLineNumber(newLineNumber);
    }

    public int setBytecodeIndex(final int newBytecodeIndex) {
        return getDelegate().setBytecodeIndex(newBytecodeIndex);
    }

    public int bytecodeIndex() {
        return getDelegate().bytecodeIndex();
    }

    public void finish() {
        getDelegate().finish();
    }

    public BasicBlock getFirstBlock() throws IllegalStateException {
        return getDelegate().getFirstBlock();
    }

    public BlockLabel getEntryLabel() throws IllegalStateException {
        return getDelegate().getEntryLabel();
    }

    public BasicBlockBuilder getDelegate() {
        return delegate;
    }

    public Value checkcast(final Value value, final Value toType, final Value toDimensions, final CheckCast.CastType kind, final ObjectType expectedType) {
        return getDelegate().checkcast(value, toType, toDimensions, kind, expectedType);
    }

    public Value checkcast(final Value value, final TypeDescriptor desc) {
        return getDelegate().checkcast(value, desc);
    }

    public Value deref(Value pointer) {
        return getDelegate().deref(pointer);
    }

    public Value invokeDynamic(final MethodMethodHandleConstant bootstrapHandle, final List<Literal> bootstrapArgs, final String name, final MethodDescriptor descriptor, List<Value> arguments) {
        return getDelegate().invokeDynamic(bootstrapHandle, bootstrapArgs, name, descriptor, arguments);
    }

    public Value currentThread() {
        return getDelegate().currentThread();
    }

    public Value memberOf(final Value structPointer, final StructType.Member member) {
        return getDelegate().memberOf(structPointer, member);
    }

    public Value memberOfUnion(Value unionPointer, UnionType.Member member) {
        return getDelegate().memberOfUnion(unionPointer, member);
    }

    public Value elementOf(final Value arrayPointer, final Value index) {
        return getDelegate().elementOf(arrayPointer, index);
    }

    public Value offsetPointer(Value basePointer, Value offset) {
        return getDelegate().offsetPointer(basePointer, offset);
    }

    public Value pointerDifference(final Value leftPointer, final Value rightPointer) {
        return getDelegate().pointerDifference(leftPointer, rightPointer);
    }

    public Value byteOffsetPointer(final Value base, final Value offset, final ValueType outputType) {
        return getDelegate().byteOffsetPointer(base, offset, outputType);
    }

    public Value lookupVirtualMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return getDelegate().lookupVirtualMethod(reference, owner, name, descriptor);
    }

    public Value lookupVirtualMethod(Value reference, InstanceMethodElement method) {
        return getDelegate().lookupVirtualMethod(reference, method);
    }

    public Value lookupInterfaceMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return getDelegate().lookupInterfaceMethod(reference, owner, name, descriptor);
    }

    public Value lookupInterfaceMethod(Value reference, InstanceMethodElement method) {
        return getDelegate().lookupInterfaceMethod(reference, method);
    }

    public Value resolveStaticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return getDelegate().resolveStaticMethod(owner, name, descriptor);
    }

    public Value resolveInstanceMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return getDelegate().resolveInstanceMethod(owner, name, descriptor);
    }

    public Value resolveConstructor(TypeDescriptor owner, MethodDescriptor descriptor) {
        return getDelegate().resolveConstructor(owner, descriptor);
    }

    public Value threadBound(Value threadPtr, Value target) {
        return getDelegate().threadBound(threadPtr, target);
    }

    public Value resolveStaticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        return getDelegate().resolveStaticField(owner, name, type);
    }

    public Value instanceFieldOf(Value instancePointer, InstanceFieldElement field) {
        return getDelegate().instanceFieldOf(instancePointer, field);
    }

    public Value instanceFieldOf(Value instancePointer, TypeDescriptor owner, String name, TypeDescriptor type) {
        return getDelegate().instanceFieldOf(instancePointer, owner, name, type);
    }

    public Value auto(Value initializer) {
        return getDelegate().auto(initializer);
    }

    public Value stackAllocate(final ValueType type, final Value count, final Value align) {
        return getDelegate().stackAllocate(type, count, align);
    }

    public BlockEntry getBlockEntry() {
        return getDelegate().getBlockEntry();
    }

    public BasicBlock getTerminatedBlock() {
        return getDelegate().getTerminatedBlock();
    }

    public Value offsetOfField(final FieldElement fieldElement) {
        return getDelegate().offsetOfField(fieldElement);
    }

    public Value extractElement(final Value array, final Value index) {
        return getDelegate().extractElement(array, index);
    }

    public Value extractMember(final Value compound, final StructType.Member member) {
        return getDelegate().extractMember(compound, member);
    }

    public Value extractInstanceField(Value valueObj, TypeDescriptor owner, String name, TypeDescriptor type) {
        return getDelegate().extractInstanceField(valueObj, owner, name, type);
    }

    public Value extractInstanceField(Value valueObj, FieldElement field) {
        return getDelegate().extractInstanceField(valueObj, field);
    }

    public Value insertElement(Value array, Value index, Value value) {
        return getDelegate().insertElement(array, index, value);
    }

    public Value insertMember(Value compound, StructType.Member member, Value value) {
        return getDelegate().insertMember(compound, member, value);
    }

    public Node declareDebugAddress(LocalVariableElement variable, Value address) {
        return getDelegate().declareDebugAddress(variable, address);
    }

    public Node setDebugValue(final LocalVariableElement variable, final Value value) {
        return getDelegate().setDebugValue(variable, value);
    }

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        return getDelegate().select(condition, trueValue, falseValue);
    }

    public Value loadLength(final Value arrayPointer) {
        return getDelegate().loadLength(arrayPointer);
    }

    public Value loadTypeId(Value objectPointer) {
        return getDelegate().loadTypeId(objectPointer);
    }

    public Value new_(final ClassObjectType type, final Value typeId, final Value size, final Value align) {
        return getDelegate().new_(type, typeId, size, align);
    }

    public Value new_(final ClassTypeDescriptor desc) {
        return getDelegate().new_(desc);
    }

    public Value newArray(final PrimitiveArrayObjectType arrayType, final Value size) {
        return getDelegate().newArray(arrayType, size);
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        return getDelegate().newArray(desc, size);
    }

    public Value newReferenceArray(final ReferenceArrayObjectType arrayType, Value elemTypeId, Value dimensions, final Value size) {
        return getDelegate().newReferenceArray(arrayType, elemTypeId, dimensions, size);
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        return getDelegate().multiNewArray(arrayType, dimensions);
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        return getDelegate().multiNewArray(desc, dimensions);
    }

    public Value load(final Value pointer, final ReadAccessMode accessMode) {
        return getDelegate().load(pointer, accessMode);
    }

    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return getDelegate().readModifyWrite(pointer, op, update, readMode, writeMode);
    }

    public Value cmpAndSwap(Value target, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        return getDelegate().cmpAndSwap(target, expect, update, readMode, writeMode, strength);
    }

    public Node store(Value handle, Value value, WriteAccessMode accessMode) {
        return getDelegate().store(handle, value, accessMode);
    }

    public Node initCheck(InitializerElement initializer, Value initThunk) {
        return getDelegate().initCheck(initializer, initThunk);
    }

    public Node initializeClass(Value classToInit) {
        return getDelegate().initializeClass(classToInit);
    }

    public Node fence(final GlobalAccessMode fenceType) {
        return getDelegate().fence(fenceType);
    }

    public Node monitorEnter(final Value obj) {
        return getDelegate().monitorEnter(obj);
    }

    public Node monitorExit(final Value obj) {
        return getDelegate().monitorExit(obj);
    }

    public Value nullCheck(Value input) {
        return getDelegate().nullCheck(input);
    }

    public Value divisorCheck(Value input) {
        return getDelegate().divisorCheck(input);
    }

    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        return getDelegate().call(targetPtr, receiver, arguments);
    }

    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        return getDelegate().callNoSideEffects(targetPtr, receiver, arguments);
    }

    public Node begin(final BlockLabel blockLabel) {
        return getDelegate().begin(blockLabel);
    }

    public <T> BasicBlock begin(BlockLabel blockLabel, T arg, BiConsumer<T, BasicBlockBuilder> maker) {
        return getDelegate().begin(blockLabel, arg, maker);
    }

    public Node reachable(final Value value) {
        return getDelegate().reachable(value);
    }

    public Node safePoint() {
        return getDelegate().safePoint();
    }

    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        return getDelegate().callNoReturn(targetPtr, receiver, arguments);
    }

    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return getDelegate().invokeNoReturn(targetPtr, receiver, arguments, catchLabel, targetArguments);
    }

    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        return getDelegate().tailCall(targetPtr, receiver, arguments);
    }

    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return getDelegate().invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
    }

    public BasicBlock goto_(final BlockLabel resumeLabel, Map<Slot, Value> args) {
        return getDelegate().goto_(resumeLabel, args);
    }

    public BasicBlock if_(Value condition, BlockLabel trueTarget, BlockLabel falseTarget, Map<Slot, Value> targetArguments) {
        return getDelegate().if_(condition, trueTarget, falseTarget, targetArguments);
    }

    public BasicBlock return_(final Value value) {
        return getDelegate().return_(value);
    }

    public BasicBlock unreachable() {
        return getDelegate().unreachable();
    }

    public BasicBlock throw_(final Value value) {
        return getDelegate().throw_(value);
    }

    public BasicBlock switch_(final Value value, final int[] checkValues, final BlockLabel[] targets, final BlockLabel defaultTarget, Map<Slot, Value> targetArguments) {
        return getDelegate().switch_(value, checkValues, targets, defaultTarget, targetArguments);
    }

    public Value add(final Value v1, final Value v2) {
        return getDelegate().add(v1, v2);
    }

    public Value multiply(final Value v1, final Value v2) {
        return getDelegate().multiply(v1, v2);
    }

    public Value and(final Value v1, final Value v2) {
        return getDelegate().and(v1, v2);
    }

    public Value or(final Value v1, final Value v2) {
        return getDelegate().or(v1, v2);
    }

    public Value xor(final Value v1, final Value v2) {
        return getDelegate().xor(v1, v2);
    }

    public Value isEq(final Value v1, final Value v2) {
        return getDelegate().isEq(v1, v2);
    }

    public Value isNe(final Value v1, final Value v2) {
        return getDelegate().isNe(v1, v2);
    }

    public Value shr(final Value v1, final Value v2) {
        return getDelegate().shr(v1, v2);
    }

    public Value shl(final Value v1, final Value v2) {
        return getDelegate().shl(v1, v2);
    }

    public Value sub(final Value v1, final Value v2) {
        return getDelegate().sub(v1, v2);
    }

    public Value divide(final Value v1, final Value v2) {
        return getDelegate().divide(v1, v2);
    }

    public Value remainder(final Value v1, final Value v2) {
        return getDelegate().remainder(v1, v2);
    }

    public Value min(final Value v1, final Value v2) {
        return getDelegate().min(v1, v2);
    }

    public Value max(final Value v1, final Value v2) {
        return getDelegate().max(v1, v2);
    }

    public Value isLt(final Value v1, final Value v2) {
        return getDelegate().isLt(v1, v2);
    }

    public Value isGt(final Value v1, final Value v2) {
        return getDelegate().isGt(v1, v2);
    }

    public Value isLe(final Value v1, final Value v2) {
        return getDelegate().isLe(v1, v2);
    }

    public Value isGe(final Value v1, final Value v2) {
        return getDelegate().isGe(v1, v2);
    }

    public Value rol(final Value v1, final Value v2) {
        return getDelegate().rol(v1, v2);
    }

    public Value ror(final Value v1, final Value v2) {
        return getDelegate().ror(v1, v2);
    }

    public Value cmp(Value v1, Value v2) {
        return getDelegate().cmp(v1, v2);
    }

    public Value cmpG(Value v1, Value v2) {
        return getDelegate().cmpG(v1, v2);
    }

    public Value cmpL(Value v1, Value v2) {
        return getDelegate().cmpL(v1, v2);
    }

    public Value notNull(final Value v) {
        return getDelegate().notNull(v);
    }

    public Value negate(final Value v) {
        return getDelegate().negate(v);
    }

    public Value complement(final Value v) {
        return getDelegate().complement(v);
    }

    public Value byteSwap(final Value v) {
        return getDelegate().byteSwap(v);
    }

    public Value bitReverse(final Value v) {
        return getDelegate().bitReverse(v);
    }

    public Value countLeadingZeros(final Value v) {
        return getDelegate().countLeadingZeros(v);
    }

    public Value countTrailingZeros(final Value v) {
        return getDelegate().countTrailingZeros(v);
    }

    public Value truncate(final Value value, final WordType toType) {
        return getDelegate().truncate(value, toType);
    }

    public Value extend(final Value value, final WordType toType) {
        return getDelegate().extend(value, toType);
    }

    public Value bitCast(final Value value, final WordType toType) {
        return getDelegate().bitCast(value, toType);
    }

    public Value fpToInt(final Value value, final IntegerType toType) {
        return getDelegate().fpToInt(value, toType);
    }

    public Value intToFp(final Value value, final FloatType toType) {
        return getDelegate().intToFp(value, toType);
    }

    public Value decodeReference(Value refVal, PointerType pointerType) {
        return getDelegate().decodeReference(refVal, pointerType);
    }

    public Value encodeReference(Value pointer, ReferenceType referenceType) {
        return getDelegate().encodeReference(pointer, referenceType);
    }

    public Value instanceOf(final Value input, final ObjectType expectedType, final int expectedDimensions) {
        return getDelegate().instanceOf(input, expectedType, expectedDimensions);
    }

    public Value instanceOf(final Value input, final TypeDescriptor desc) {
        return getDelegate().instanceOf(input, desc);
    }

    public Value populationCount(final Value v) {
        return getDelegate().populationCount(v);
    }

    public BasicBlock ret(final Value address, Map<Slot, Value> targetArguments) {
        return getDelegate().ret(address, targetArguments);
    }

    public Node nop() {
        return getDelegate().nop();
    }

    public Value classOf(final Value typeId, final Value dimensions) {
        return getDelegate().classOf(typeId, dimensions);
    }

    public Value vaArg(Value vaList, ValueType type) {
        return getDelegate().vaArg(vaList, type);
    }
}
