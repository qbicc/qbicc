package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

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

    public PhiValue phi(final Type type, final BasicBlock basicBlock) {
        return getDelegate().phi(type, basicBlock);
    }

    public PhiValue phi(final Type type, final NodeHandle basicBlockHandle) {
        return getDelegate().phi(type, basicBlockHandle);
    }

    public Value if_(final Context ctxt, final Value condition, final Value trueValue, final Value falseValue) {
        return getDelegate().if_(ctxt, condition, trueValue, falseValue);
    }

    public Value lengthOfArray(final Context ctxt, final Value array) {
        return getDelegate().lengthOfArray(ctxt, array);
    }

    public Value instanceOf(final Context ctxt, final Value v, final ClassType type) {
        return getDelegate().instanceOf(ctxt, v, type);
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

    public Value readInstanceField(final Context ctxt, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
        return getDelegate().readInstanceField(ctxt, instance, owner, name, mode);
    }

    public Value readStaticField(final Context ctxt, final ClassType owner, final String name, final JavaAccessMode mode) {
        return getDelegate().readStaticField(ctxt, owner, name, mode);
    }

    public Value readArrayValue(final Context ctxt, final Value array, final Value index, final JavaAccessMode mode) {
        return getDelegate().readArrayValue(ctxt, array, index, mode);
    }

    public Node pointerStore(final Context ctxt, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return getDelegate().pointerStore(ctxt, pointer, value, accessMode, atomicityMode);
    }

    public Node writeInstanceField(final Context ctxt, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeInstanceField(ctxt, instance, owner, name, value, mode);
    }

    public Node writeStaticField(final Context ctxt, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return getDelegate().writeStaticField(ctxt, owner, name, value, mode);
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

    public Node invokeMethod(final Context ctxt, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return getDelegate().invokeMethod(ctxt, owner, method, arguments);
    }

    public Node invokeInstanceMethod(final Context ctxt, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return getDelegate().invokeInstanceMethod(ctxt, instance, kind, owner, method, arguments);
    }

    public Value invokeValueMethod(final Context ctxt, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return getDelegate().invokeValueMethod(ctxt, owner, method, arguments);
    }

    public Value invokeInstanceValueMethod(final Context ctxt, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return getDelegate().invokeInstanceValueMethod(ctxt, instance, kind, owner, method, arguments);
    }

    public BasicBlock goto_(final Context ctxt, final NodeHandle targetHandle) {
        return getDelegate().goto_(ctxt, targetHandle);
    }

    public BasicBlock if_(final Context ctxt, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
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

    public BasicBlock switch_(final Context ctxt, final Value value, final int[] checkValues, final NodeHandle[] targets, final NodeHandle defaultTarget) {
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

    public BasicBlock jsr(final Context ctxt, final NodeHandle target, final NodeHandle ret) {
        return getDelegate().jsr(ctxt, target, ret);
    }

    public BasicBlock ret(final Context ctxt, final Value address) {
        return getDelegate().ret(ctxt, address);
    }
}
