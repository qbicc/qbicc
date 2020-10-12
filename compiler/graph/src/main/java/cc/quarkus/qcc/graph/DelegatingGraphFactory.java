package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * A graph factory which delegates all operations to another graph factory.  Can be used as a base class for graph
 * modifying plugins.
 */
public class DelegatingGraphFactory implements GraphFactory {
    private final GraphFactory delegate;

    public DelegatingGraphFactory(final GraphFactory delegate) {
        this.delegate = delegate;
    }

    public GraphFactory getDelegate() {
        return delegate;
    }

    public Value parameter(final Type type, final int index) {
        return getDelegate().parameter(type, index);
    }

    public PhiValue phi(final Context ctxt, final Type type) {
        return getDelegate().phi(ctxt, type);
    }

    public Value if_(final Context ctxt, final Value condition, final Value trueValue, final Value falseValue) {
        return getDelegate().if_(ctxt, condition, trueValue, falseValue);
    }

    public Value arrayLength(final Context ctxt, final Value array) {
        return getDelegate().arrayLength(ctxt, array);
    }

    public Value instanceOf(final Context ctxt, final Value value, final ClassType type) {
        return getDelegate().instanceOf(ctxt, value, type);
    }

    public Value new_(final Context ctxt, final ClassType type) {
        return getDelegate().new_(ctxt, type);
    }

    public Value newArray(final Context ctxt, final ArrayType type, final Value size) {
        return getDelegate().newArray(ctxt, type, size);
    }

    public Value multiNewArray(final Context ctxt, final ArrayType type, final Value... dimensions) {
        return getDelegate().multiNewArray(ctxt, type, dimensions);
    }

    public Value clone(final Context ctxt, final Value object) {
        return getDelegate().clone(ctxt, object);
    }

    public Value pointerLoad(final Context ctxt, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return getDelegate().pointerLoad(ctxt, pointer, accessMode, atomicityMode);
    }

    public Value readInstanceField(final Context ctxt, final Value instance, final FieldElement fieldElement, final JavaAccessMode mode) {
        return getDelegate().readInstanceField(ctxt, instance, fieldElement, mode);
    }

    public Value readStaticField(final Context ctxt, final FieldElement fieldElement, final JavaAccessMode mode) {
        return getDelegate().readStaticField(ctxt, fieldElement, mode);
    }

    public Value readArrayValue(final Context ctxt, final Value array, final Value index, final JavaAccessMode mode) {
        return getDelegate().readArrayValue(ctxt, array, index, mode);
    }

    public Node pointerStore(final Context ctxt, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return getDelegate().pointerStore(ctxt, pointer, value, accessMode, atomicityMode);
    }

    public Node writeInstanceField(final Context ctxt, final Value instance, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeInstanceField(ctxt, instance, fieldElement, value, mode);
    }

    public Node writeStaticField(final Context ctxt, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeStaticField(ctxt, fieldElement, value, mode);
    }

    public Node writeArrayValue(final Context ctxt, final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeArrayValue(ctxt, array, index, value, mode);
    }

    public Node fence(final Context ctxt, final MemoryAtomicityMode fenceType) {
        return getDelegate().fence(ctxt, fenceType);
    }

    public Node monitorEnter(final Context ctxt, final Value obj) {
        return getDelegate().monitorEnter(ctxt, obj);
    }

    public Node monitorExit(final Context ctxt, final Value obj) {
        return getDelegate().monitorExit(ctxt, obj);
    }

    public Node invokeStatic(final Context ctxt, final MethodElement target, final List<Value> arguments) {
        return getDelegate().invokeStatic(ctxt, target, arguments);
    }

    public Node invokeInstance(final Context ctxt, final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        return getDelegate().invokeInstance(ctxt, kind, instance, target, arguments);
    }

    public Value invokeValueStatic(final Context ctxt, final MethodElement target, final List<Value> arguments) {
        return getDelegate().invokeValueStatic(ctxt, target, arguments);
    }

    public Value invokeInstanceValueMethod(final Context ctxt, final Value instance, final DispatchInvocation.Kind kind, final MethodElement target, final List<Value> arguments) {
        return getDelegate().invokeInstanceValueMethod(ctxt, instance, kind, target, arguments);
    }

    public Node begin(final Context ctxt, final BlockLabel blockLabel) {
        return getDelegate().begin(ctxt, blockLabel);
    }

    public BasicBlock goto_(final Context ctxt, final BlockLabel resumeLabel) {
        return getDelegate().goto_(ctxt, resumeLabel);
    }

    public BasicBlock if_(final Context ctxt, final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget) {
        return getDelegate().if_(ctxt, condition, trueTarget, falseTarget);
    }

    public BasicBlock return_(final Context ctxt) {
        return getDelegate().return_(ctxt);
    }

    public BasicBlock return_(final Context ctxt, final Value value) {
        return getDelegate().return_(ctxt, value);
    }

    public BasicBlock throw_(final Context ctxt, final Value value) {
        return getDelegate().throw_(ctxt, value);
    }

    public BasicBlock switch_(final Context ctxt, final Value value, final int[] checkValues, final BlockLabel[] targets, final BlockLabel defaultTarget) {
        return getDelegate().switch_(ctxt, value, checkValues, targets, defaultTarget);
    }

    public Value add(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().add(ctxt, v1, v2);
    }

    public Value multiply(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().multiply(ctxt, v1, v2);
    }

    public Value and(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().and(ctxt, v1, v2);
    }

    public Value or(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().or(ctxt, v1, v2);
    }

    public Value xor(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().xor(ctxt, v1, v2);
    }

    public Value cmpEq(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().cmpEq(ctxt, v1, v2);
    }

    public Value cmpNe(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().cmpNe(ctxt, v1, v2);
    }

    public Value shr(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().shr(ctxt, v1, v2);
    }

    public Value shl(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().shl(ctxt, v1, v2);
    }

    public Value sub(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().sub(ctxt, v1, v2);
    }

    public Value divide(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().divide(ctxt, v1, v2);
    }

    public Value remainder(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().remainder(ctxt, v1, v2);
    }

    public Value cmpLt(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().cmpLt(ctxt, v1, v2);
    }

    public Value cmpGt(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().cmpGt(ctxt, v1, v2);
    }

    public Value cmpLe(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().cmpLe(ctxt, v1, v2);
    }

    public Value cmpGe(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().cmpGe(ctxt, v1, v2);
    }

    public Value rol(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().rol(ctxt, v1, v2);
    }

    public Value ror(final Context ctxt, final Value v1, final Value v2) {
        return getDelegate().ror(ctxt, v1, v2);
    }

    public Value negate(final Context ctxt, final Value v) {
        return getDelegate().negate(ctxt, v);
    }

    public Value byteSwap(final Context ctxt, final Value v) {
        return getDelegate().byteSwap(ctxt, v);
    }

    public Value bitReverse(final Context ctxt, final Value v) {
        return getDelegate().bitReverse(ctxt, v);
    }

    public Value countLeadingZeros(final Context ctxt, final Value v) {
        return getDelegate().countLeadingZeros(ctxt, v);
    }

    public Value countTrailingZeros(final Context ctxt, final Value v) {
        return getDelegate().countTrailingZeros(ctxt, v);
    }

    public Value truncate(final Context ctxt, final Value value, final WordType toType) {
        return getDelegate().truncate(ctxt, value, toType);
    }

    public Value extend(final Context ctxt, final Value value, final WordType toType) {
        return getDelegate().extend(ctxt, value, toType);
    }

    public Value bitCast(final Context ctxt, final Value value, final WordType toType) {
        return getDelegate().bitCast(ctxt, value, toType);
    }

    public Value valueConvert(final Context ctxt, final Value value, final WordType toType) {
        return getDelegate().valueConvert(ctxt, value, toType);
    }

    public Value populationCount(final Context ctxt, final Value v) {
        return getDelegate().populationCount(ctxt, v);
    }

    public BasicBlock jsr(final Context ctxt, final BlockLabel subLabel, final BlockLabel resumeLabel) {
        return getDelegate().jsr(ctxt, subLabel, resumeLabel);
    }

    public BasicBlock ret(final Context ctxt, final Value address) {
        return getDelegate().ret(ctxt, address);
    }

    public Value receiver(final ClassType type) {
        return getDelegate().receiver(type);
    }

    public Value catch_(final Context ctxt, final ClassType type) {
        return getDelegate().catch_(ctxt, type);
    }

    public Value invokeConstructor(final Context ctxt, final Value instance, final ConstructorElement target, final List<Value> arguments) {
        return getDelegate().invokeConstructor(ctxt, instance, target, arguments);
    }
}
