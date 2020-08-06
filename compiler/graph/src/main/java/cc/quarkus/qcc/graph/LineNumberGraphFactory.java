package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

/**
 * A graph factory which sets the line number on each created node.
 */
public class LineNumberGraphFactory extends DelegatingGraphFactory {
    private int lineNumber;

    public LineNumberGraphFactory(final GraphFactory delegate) {
        super(delegate);
    }

    private <N> N withLineNumber(N orig) {
        if (orig instanceof Node && lineNumber != 0) {
            ((Node) orig).setSourceLine(lineNumber);
        }
        return orig;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public PhiValue phi(final Type type, final BasicBlock basicBlock) {
        return withLineNumber(getDelegate().phi(type, basicBlock));
    }

    public Value if_(final Value condition, final Value trueValue, final Value falseValue) {
        return withLineNumber(getDelegate().if_(condition, trueValue, falseValue));
    }

    public Value binaryOperation(final CommutativeBinaryValue.Kind kind, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().binaryOperation(kind, v1, v2));
    }

    public Value binaryOperation(final NonCommutativeBinaryValue.Kind kind, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().binaryOperation(kind, v1, v2));
    }

    public Value unaryOperation(final UnaryValue.Kind kind, final Value v) {
        return withLineNumber(getDelegate().unaryOperation(kind, v));
    }

    public Value lengthOfArray(final Value array) {
        return withLineNumber(getDelegate().lengthOfArray(array));
    }

    public Value instanceOf(final Value v, final ClassType type) {
        return withLineNumber(getDelegate().instanceOf(v, type));
    }

    public Value reinterpretCast(final Value v, final Type type) {
        return withLineNumber(getDelegate().reinterpretCast(v, type));
    }

    public PhiDependency phiDependency(final BasicBlock basicBlock) {
        return withLineNumber(getDelegate().phiDependency(basicBlock));
    }

    public Value castOperation(final WordCastValue.Kind kind, final Value value, final WordType toType) {
        return withLineNumber(getDelegate().castOperation(kind, value, toType));
    }

    public Value new_(final Node dependency, final ClassType type) {
        return withLineNumber(getDelegate().new_(dependency, type));
    }

    public Value newArray(final Node dependency, final ArrayType type, final Value size) {
        return withLineNumber(getDelegate().newArray(dependency, type, size));
    }

    public Value pointerLoad(final Node dependency, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return withLineNumber(getDelegate().pointerLoad(dependency, pointer, accessMode, atomicityMode));
    }

    public Value readInstanceField(final Node dependency, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readInstanceField(dependency, instance, owner, name, mode));
    }

    public Value readStaticField(final Node dependency, final ClassType owner, final String name, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readStaticField(dependency, owner, name, mode));
    }

    public Value readArrayValue(final Node dependency, final Value array, final Value index, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readArrayValue(dependency, array, index, mode));
    }

    public Node pointerStore(final Node dependency, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return withLineNumber(getDelegate().pointerStore(dependency, pointer, value, accessMode, atomicityMode));
    }

    public Node writeInstanceField(final Node dependency, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeInstanceField(dependency, instance, owner, name, value, mode));
    }

    public Node writeStaticField(final Node dependency, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeStaticField(dependency, owner, name, value, mode));
    }

    public Node writeArrayValue(final Node dependency, final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeArrayValue(dependency, array, index, value, mode));
    }

    public Node fence(final Node dependency, final MemoryAtomicityMode fenceType) {
        return withLineNumber(getDelegate().fence(dependency, fenceType));
    }

    public Node invokeMethod(final Node dependency, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeMethod(dependency, owner, method, arguments));
    }

    public Node invokeInstanceMethod(final Node dependency, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeInstanceMethod(dependency, instance, kind, owner, method, arguments));
    }

    public Value invokeValueMethod(final Node dependency, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeValueMethod(dependency, owner, method, arguments));
    }

    public Value invokeInstanceValueMethod(final Node dependency, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeInstanceValueMethod(dependency, instance, kind, owner, method, arguments));
    }

    public Terminator goto_(final Node dependency, final NodeHandle targetHandle) {
        return withLineNumber(getDelegate().goto_(dependency, targetHandle));
    }

    public Terminator if_(final Node dependency, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
        return withLineNumber(getDelegate().if_(dependency, condition, trueTarget, falseTarget));
    }

    public Terminator return_(final Node dependency) {
        return withLineNumber(getDelegate().return_(dependency));
    }

    public Terminator return_(final Node dependency, final Value value) {
        return withLineNumber(getDelegate().return_(dependency, value));
    }

    public Terminator throw_(final Node dependency, final Value value) {
        return withLineNumber(getDelegate().throw_(dependency, value));
    }

    public Terminator switch_(final Node dependency, final Value value, final int[] keys, final NodeHandle[] targets, final NodeHandle defaultTarget) {
        return withLineNumber(getDelegate().switch_(dependency, value, keys, targets, defaultTarget));
    }

    public Terminator tryInvokeMethod(final Node dependency, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(getDelegate().tryInvokeMethod(dependency, owner, method, arguments, returnTarget, catchTarget));
    }

    public Terminator tryInvokeInstanceMethod(final Node dependency, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(getDelegate().tryInvokeInstanceMethod(dependency, instance, kind, owner, method, arguments, returnTarget, catchTarget));
    }

    public ValueTerminator tryInvokeValueMethod(final Node dependency, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(getDelegate().tryInvokeValueMethod(dependency, owner, method, arguments, returnTarget, catchTarget));
    }

    public ValueTerminator tryInvokeInstanceValueMethod(final Node dependency, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(getDelegate().tryInvokeInstanceValueMethod(dependency, instance, kind, owner, method, arguments, returnTarget, catchTarget));
    }

    public Terminator tryThrow(final Node dependency, final Value value, final NodeHandle catchTarget) {
        return withLineNumber(getDelegate().tryThrow(dependency, value, catchTarget));
    }

    public BasicBlock block(final Terminator term) {
        return withLineNumber(getDelegate().block(term));
    }
}
