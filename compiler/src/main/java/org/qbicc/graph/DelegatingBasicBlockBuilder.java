package org.qbicc.graph;

import java.util.List;

import org.qbicc.context.Location;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
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

    public BasicBlockBuilder getFirstBuilder() {
        return last.getFirstBuilder();
    }

    public void setFirstBuilder(final BasicBlockBuilder first) {
        last.setFirstBuilder(first);
    }

    public ExecutableElement getCurrentElement() {
        return getDelegate().getCurrentElement();
    }

    public ExecutableElement getRootElement() {
        return getDelegate().getRootElement();
    }

    public ExecutableElement setCurrentElement(final ExecutableElement element) {
        return getDelegate().setCurrentElement(element);
    }

    public Node getCallSite() {
        return getDelegate().getCallSite();
    }

    public Node setCallSite(final Node callSite) {
        return getDelegate().setCallSite(callSite);
    }

    public Location getLocation() {
        return getDelegate().getLocation();
    }

    public int setLineNumber(final int newLineNumber) {
        return getDelegate().setLineNumber(newLineNumber);
    }

    public int setBytecodeIndex(final int newBytecodeIndex) {
        return getDelegate().setBytecodeIndex(newBytecodeIndex);
    }

    public final ExceptionHandler getExceptionHandler() {
        return last.getExceptionHandler();
    }

    public void setExceptionHandlerPolicy(final ExceptionHandlerPolicy policy) {
        getDelegate().setExceptionHandlerPolicy(policy);
    }

    public void startMethod(List<ParameterValue> arguments) {
        getDelegate().startMethod(arguments);
    }

    public void finish() {
        getDelegate().finish();
    }

    public BasicBlock getFirstBlock() throws IllegalStateException {
        return getDelegate().getFirstBlock();
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

    public ValueHandle memberOf(final ValueHandle structHandle, final CompoundType.Member member) {
        return getDelegate().memberOf(structHandle, member);
    }

    public ValueHandle elementOf(final ValueHandle array, final Value index) {
        return getDelegate().elementOf(array, index);
    }

    public ValueHandle unsafeHandle(final ValueHandle base, final Value offset, final ValueType outputType) {
        return getDelegate().unsafeHandle(base, offset, outputType);
    }

    public ValueHandle pointerHandle(Value pointer) {
        return getDelegate().pointerHandle(pointer);
    }

    public ValueHandle referenceHandle(Value reference) {
        return getDelegate().referenceHandle(reference);
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field) {
        return getDelegate().instanceFieldOf(instance, field);
    }

    public ValueHandle instanceFieldOf(ValueHandle instance, TypeDescriptor owner, String name, TypeDescriptor type) {
        return getDelegate().instanceFieldOf(instance, owner, name, type);
    }

    public ValueHandle staticField(FieldElement field) {
        return getDelegate().staticField(field);
    }

    public ValueHandle staticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        return getDelegate().staticField(owner, name, type);
    }

    public ValueHandle globalVariable(GlobalVariableElement variable) {
        return getDelegate().globalVariable(variable);
    }

    public ValueHandle localVariable(LocalVariableElement variable) {
        return getDelegate().localVariable(variable);
    }

    public ValueHandle exactMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return getDelegate().exactMethodOf(instance, method, callSiteDescriptor, callSiteType);
    }

    public ValueHandle exactMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return getDelegate().exactMethodOf(instance, owner, name, descriptor);
    }

    public ValueHandle virtualMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return getDelegate().virtualMethodOf(instance, method, callSiteDescriptor, callSiteType);
    }

    public ValueHandle virtualMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return getDelegate().virtualMethodOf(instance, owner, name, descriptor);
    }

    public ValueHandle interfaceMethodOf(Value instance, MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return getDelegate().interfaceMethodOf(instance, method, callSiteDescriptor, callSiteType);
    }

    public ValueHandle interfaceMethodOf(Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return getDelegate().interfaceMethodOf(instance, owner, name, descriptor);
    }

    public ValueHandle staticMethod(MethodElement method, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return getDelegate().staticMethod(method, callSiteDescriptor, callSiteType);
    }

    public ValueHandle staticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return getDelegate().staticMethod(owner, name, descriptor);
    }

    public ValueHandle constructorOf(Value instance, ConstructorElement constructor, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        return getDelegate().constructorOf(instance, constructor, callSiteDescriptor, callSiteType);
    }

    public ValueHandle constructorOf(Value instance, TypeDescriptor owner, MethodDescriptor descriptor) {
        return getDelegate().constructorOf(instance, owner, descriptor);
    }

    public ValueHandle functionOf(FunctionElement function) {
        return getDelegate().functionOf(function);
    }

    public ValueHandle functionOf(Function function) {
        return getDelegate().functionOf(function);
    }

    public ValueHandle functionOf(FunctionDeclaration function) {
        return getDelegate().functionOf(function);
    }

    public Value addressOf(final ValueHandle handle) {
        return getDelegate().addressOf(handle);
    }

    public Value referenceTo(ValueHandle handle) throws IllegalArgumentException {
        return getDelegate().referenceTo(handle);
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

    public ParameterValue parameter(final ValueType type, String label, final int index) {
        return getDelegate().parameter(type, label, index);
    }

    public Value currentThread() {
        return getDelegate().currentThread();
    }

    public Value offsetOfField(final FieldElement fieldElement) {
        return getDelegate().offsetOfField(fieldElement);
    }

    public Value extractElement(final Value array, final Value index) {
        return getDelegate().extractElement(array, index);
    }

    public Value extractMember(final Value compound, final CompoundType.Member member) {
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

    public Value insertMember(Value compound, CompoundType.Member member, Value value) {
        return getDelegate().insertMember(compound, member, value);
    }

    public Node declareDebugAddress(LocalVariableElement variable, Value address) {
        return getDelegate().declareDebugAddress(variable, address);
    }

    public PhiValue phi(final ValueType type, final BlockLabel owner, PhiValue.Flag... flags) {
        return getDelegate().phi(type, owner, flags);
    }

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        return getDelegate().select(condition, trueValue, falseValue);
    }

    public Value arrayLength(final ValueHandle arrayHandle) {
        return getDelegate().arrayLength(arrayHandle);
    }

    public Value new_(final ClassObjectType type) {
        return getDelegate().new_(type);
    }

    public Value new_(final ClassTypeDescriptor desc) {
        return getDelegate().new_(desc);
    }

    public Value newArray(final ArrayObjectType arrayType, final Value size) {
        return getDelegate().newArray(arrayType, size);
    }

    public Value newArray(final ArrayTypeDescriptor desc, final Value size) {
        return getDelegate().newArray(desc, size);
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        return getDelegate().multiNewArray(arrayType, dimensions);
    }

    public Value multiNewArray(final ArrayTypeDescriptor desc, final List<Value> dimensions) {
        return getDelegate().multiNewArray(desc, dimensions);
    }

    public Value clone(final Value object) {
        return getDelegate().clone(object);
    }

    public Value load(final ValueHandle handle, final MemoryAtomicityMode mode) {
        return getDelegate().load(handle, mode);
    }

    public Value getAndAdd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return getDelegate().getAndAdd(target, update, atomicityMode);
    }

    public Value getAndBitwiseAnd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return getDelegate().getAndBitwiseAnd(target, update, atomicityMode);
    }

    public Value getAndBitwiseNand(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return getDelegate().getAndBitwiseNand(target, update, atomicityMode);
    }

    public Value getAndBitwiseOr(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return getDelegate().getAndBitwiseOr(target, update, atomicityMode);
    }

    public Value getAndBitwiseXor(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return getDelegate().getAndBitwiseXor(target, update, atomicityMode);
    }

    public Value getAndSet(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return getDelegate().getAndSet(target, update, atomicityMode);
    }

    public Value getAndSetMax(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return getDelegate().getAndSetMax(target, update, atomicityMode);
    }

    public Value getAndSetMin(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return getDelegate().getAndSetMin(target, update, atomicityMode);
    }

    public Value getAndSub(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return getDelegate().getAndSub(target, update, atomicityMode);
    }

    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode, CmpAndSwap.Strength strength) {
        return getDelegate().cmpAndSwap(target, expect, update, successMode, failureMode, strength);
    }

    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        return getDelegate().store(handle, value, mode);
    }

    public Node classInitCheck(final ObjectType objectType) {
        return getDelegate().classInitCheck(objectType);
    }

    public Node fence(final MemoryAtomicityMode fenceType) {
        return getDelegate().fence(fenceType);
    }

    public Node monitorEnter(final Value obj) {
        return getDelegate().monitorEnter(obj);
    }

    public Node monitorExit(final Value obj) {
        return getDelegate().monitorExit(obj);
    }

    public Value call(ValueHandle target, List<Value> arguments) {
        return getDelegate().call(target, arguments);
    }

    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        return getDelegate().callNoSideEffects(target, arguments);
    }

    public Node begin(final BlockLabel blockLabel) {
        return getDelegate().begin(blockLabel);
    }

    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        return getDelegate().callNoReturn(target, arguments);
    }

    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        return getDelegate().invokeNoReturn(target, arguments, catchLabel);
    }

    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        return getDelegate().tailCall(target, arguments);
    }

    public BasicBlock tailInvoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel) {
        return getDelegate().tailInvoke(target, arguments, catchLabel);
    }

    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        return getDelegate().invoke(target, arguments, catchLabel, resumeLabel);
    }

    public BasicBlock goto_(final BlockLabel resumeLabel) {
        return getDelegate().goto_(resumeLabel);
    }

    public BasicBlock if_(final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget) {
        return getDelegate().if_(condition, trueTarget, falseTarget);
    }

    public BasicBlock return_() {
        return getDelegate().return_();
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

    public BasicBlock switch_(final Value value, final int[] checkValues, final BlockLabel[] targets, final BlockLabel defaultTarget) {
        return getDelegate().switch_(value, checkValues, targets, defaultTarget);
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

    public Value valueConvert(final Value value, final WordType toType) {
        return getDelegate().valueConvert(value, toType);
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

    public BasicBlock jsr(final BlockLabel subLabel, final BlockLiteral returnAddress) {
        return getDelegate().jsr(subLabel, returnAddress);
    }

    public BasicBlock ret(final Value address) {
        return getDelegate().ret(address);
    }

    public Node nop() {
        return getDelegate().nop();
    }

    public Value typeIdOf(final ValueHandle valueHandle) {
        return getDelegate().typeIdOf(valueHandle);
    }

    public Value classOf(final Value typeId, final Value dimensions) {
        return getDelegate().classOf(typeId, dimensions);
    }

    public Value deref(Value value) {
        return getDelegate().deref(value);
    }
}
