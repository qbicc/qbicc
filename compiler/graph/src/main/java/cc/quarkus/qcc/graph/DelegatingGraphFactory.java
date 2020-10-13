package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.graph.literal.ArrayTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * A graph factory which delegates all operations to another graph factory.  Can be used as a base class for graph
 * modifying plugins.
 */
public class DelegatingGraphFactory implements BasicBlockBuilder {
    private final BasicBlockBuilder delegate;

    public DelegatingGraphFactory(final BasicBlockBuilder delegate) {
        this.delegate = delegate;
    }

    public BasicBlockBuilder getDelegate() {
        return delegate;
    }

    public Value narrow(final Value value, final TypeIdLiteral toType) {
        return getDelegate().narrow(value, toType);
    }

    public BasicBlock classCastException(final Value fromType, final Value toType) {
        return getDelegate().classCastException(fromType, toType);
    }

    public Value parameter(final ValueType type, final int index) {
        return getDelegate().parameter(type, index);
    }

    public PhiValue phi(final ValueType type) {
        return getDelegate().phi(type);
    }

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        return getDelegate().select(condition, trueValue, falseValue);
    }

    public Value arrayLength(final Value array) {
        return getDelegate().arrayLength(array);
    }

    public Value new_(final ClassTypeIdLiteral typeId) {
        return getDelegate().new_(typeId);
    }

    public Value newArray(final ArrayTypeIdLiteral arrayTypeId, final Value size) {
        return getDelegate().newArray(arrayTypeId, size);
    }

    public Value multiNewArray(final ArrayTypeIdLiteral arrayTypeId, final Value... dimensions) {
        return getDelegate().multiNewArray(arrayTypeId, dimensions);
    }

    public Value clone(final Value object) {
        return getDelegate().clone(object);
    }

    public Value pointerLoad(final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return getDelegate().pointerLoad(pointer, accessMode, atomicityMode);
    }

    public Value readInstanceField(final Value instance, final FieldElement fieldElement, final JavaAccessMode mode) {
        return getDelegate().readInstanceField(instance, fieldElement, mode);
    }

    public Value readStaticField(final FieldElement fieldElement, final JavaAccessMode mode) {
        return getDelegate().readStaticField(fieldElement, mode);
    }

    public Value readArrayValue(final Value array, final Value index, final JavaAccessMode mode) {
        return getDelegate().readArrayValue(array, index, mode);
    }

    public Node pointerStore(final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return getDelegate().pointerStore(pointer, value, accessMode, atomicityMode);
    }

    public Node writeInstanceField(final Value instance, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeInstanceField(instance, fieldElement, value, mode);
    }

    public Node writeStaticField(final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeStaticField(fieldElement, value, mode);
    }

    public Node writeArrayValue(final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeArrayValue(array, index, value, mode);
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

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        return getDelegate().invokeStatic(target, arguments);
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        return getDelegate().invokeInstance(kind, instance, target, arguments);
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        return getDelegate().invokeValueStatic(target, arguments);
    }

    public Value invokeInstanceValueMethod(final Value instance, final DispatchInvocation.Kind kind, final MethodElement target, final List<Value> arguments) {
        return getDelegate().invokeInstanceValueMethod(instance, kind, target, arguments);
    }

    public Node begin(final BlockLabel blockLabel) {
        return getDelegate().begin(blockLabel);
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

    public Value cmpEq(final Value v1, final Value v2) {
        return getDelegate().cmpEq(v1, v2);
    }

    public Value cmpNe(final Value v1, final Value v2) {
        return getDelegate().cmpNe(v1, v2);
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

    public Value cmpLt(final Value v1, final Value v2) {
        return getDelegate().cmpLt(v1, v2);
    }

    public Value cmpGt(final Value v1, final Value v2) {
        return getDelegate().cmpGt(v1, v2);
    }

    public Value cmpLe(final Value v1, final Value v2) {
        return getDelegate().cmpLe(v1, v2);
    }

    public Value cmpGe(final Value v1, final Value v2) {
        return getDelegate().cmpGe(v1, v2);
    }

    public Value rol(final Value v1, final Value v2) {
        return getDelegate().rol(v1, v2);
    }

    public Value ror(final Value v1, final Value v2) {
        return getDelegate().ror(v1, v2);
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

    public Value populationCount(final Value v) {
        return getDelegate().populationCount(v);
    }

    public BasicBlock jsr(final BlockLabel subLabel, final BlockLiteral returnAddress) {
        return getDelegate().jsr(subLabel, returnAddress);
    }

    public BasicBlock ret(final Value address) {
        return getDelegate().ret(address);
    }

    public Value receiver(final TypeIdLiteral upperBound) {
        return getDelegate().receiver(upperBound);
    }

    public Value catch_(final TypeIdLiteral upperBound) {
        return getDelegate().catch_(upperBound);
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        return getDelegate().invokeConstructor(instance, target, arguments);
    }

    public Node nop() {
        return getDelegate().nop();
    }

    public Value typeIdOf(final Value value) {
        return getDelegate().typeIdOf(value);
    }

    public BasicBlock try_(final Triable operation, final ClassTypeIdLiteral[] catchTypeIds, final BlockLabel[] catchTargetLabels, final BlockLabel resumeLabel) {
        return getDelegate().try_(operation, catchTypeIds, catchTargetLabels, resumeLabel);
    }
}
