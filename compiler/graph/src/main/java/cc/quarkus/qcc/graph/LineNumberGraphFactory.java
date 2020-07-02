package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

/**
 * A graph factory which sets the line number on each created node.
 */
public class LineNumberGraphFactory implements GraphFactory {
    private final GraphFactory delegate;
    private int lineNumber;

    public LineNumberGraphFactory(final GraphFactory delegate) {
        this.delegate = delegate;
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
        return withLineNumber(delegate.phi(type, basicBlock));
    }

    public Value if_(final Value condition, final Value trueValue, final Value falseValue) {
        return withLineNumber(delegate.if_(condition, trueValue, falseValue));
    }

    public Value binaryOperation(final CommutativeBinaryValue.Kind kind, final Value v1, final Value v2) {
        return withLineNumber(delegate.binaryOperation(kind, v1, v2));
    }

    public Value binaryOperation(final NonCommutativeBinaryValue.Kind kind, final Value v1, final Value v2) {
        return withLineNumber(delegate.binaryOperation(kind, v1, v2));
    }

    public Value unaryOperation(final UnaryValue.Kind kind, final Value v) {
        return withLineNumber(delegate.unaryOperation(kind, v));
    }

    public Value lengthOfArray(final Value array) {
        return withLineNumber(delegate.lengthOfArray(array));
    }

    public Value instanceOf(final Value v, final ClassType type) {
        return withLineNumber(delegate.instanceOf(v, type));
    }

    public Value reinterpretCast(final Value v, final Type type) {
        return withLineNumber(delegate.reinterpretCast(v, type));
    }

    public Value castOperation(final WordCastValue.Kind kind, final Value value, final WordType toType) {
        return withLineNumber(delegate.castOperation(kind, value, toType));
    }

    public MemoryStateValue new_(final MemoryState input, final ClassType type) {
        return withLineNumber(delegate.new_(input, type));
    }

    public MemoryStateValue newArray(final MemoryState input, final ArrayType type, final Value size) {
        return withLineNumber(delegate.newArray(input, type, size));
    }

    public MemoryStateValue pointerLoad(final MemoryState input, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return withLineNumber(delegate.pointerLoad(input, pointer, accessMode, atomicityMode));
    }

    public MemoryStateValue readInstanceField(final MemoryState input, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
        return withLineNumber(delegate.readInstanceField(input, instance, owner, name, mode));
    }

    public MemoryStateValue readStaticField(final MemoryState input, final ClassType owner, final String name, final JavaAccessMode mode) {
        return withLineNumber(delegate.readStaticField(input, owner, name, mode));
    }

    public MemoryStateValue readArrayValue(final MemoryState input, final Value array, final Value index, final JavaAccessMode mode) {
        return withLineNumber(delegate.readArrayValue(input, array, index, mode));
    }

    public MemoryState pointerStore(final MemoryState input, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return withLineNumber(delegate.pointerStore(input, pointer, value, accessMode, atomicityMode));
    }

    public MemoryState writeInstanceField(final MemoryState input, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return withLineNumber(delegate.writeInstanceField(input, instance, owner, name, value, mode));
    }

    public MemoryState writeStaticField(final MemoryState input, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return withLineNumber(delegate.writeStaticField(input, owner, name, value, mode));
    }

    public MemoryState writeArrayValue(final MemoryState input, final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        return withLineNumber(delegate.writeArrayValue(input, array, index, value, mode));
    }

    public MemoryState fence(final MemoryState input, final MemoryAtomicityMode fenceType) {
        return withLineNumber(delegate.fence(input, fenceType));
    }

    public MemoryState invokeMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(delegate.invokeMethod(input, owner, method, arguments));
    }

    public MemoryState invokeInstanceMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(delegate.invokeInstanceMethod(input, instance, kind, owner, method, arguments));
    }

    public MemoryStateValue invokeValueMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(delegate.invokeValueMethod(input, owner, method, arguments));
    }

    public MemoryStateValue invokeInstanceValueMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(delegate.invokeInstanceValueMethod(input, instance, kind, owner, method, arguments));
    }

    public Terminator goto_(final MemoryState input, final NodeHandle targetHandle) {
        return withLineNumber(delegate.goto_(input, targetHandle));
    }

    public Terminator if_(final MemoryState input, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
        return withLineNumber(delegate.if_(input, condition, trueTarget, falseTarget));
    }

    public Terminator return_(final MemoryState input) {
        return withLineNumber(delegate.return_(input));
    }

    public Terminator return_(final MemoryState input, final Value value) {
        return withLineNumber(delegate.return_(input, value));
    }

    public Terminator throw_(final MemoryState input, final Value value) {
        return withLineNumber(delegate.throw_(input, value));
    }

    public Terminator switch_(final MemoryState input, final Value value, final int[] keys, final NodeHandle[] targets, final NodeHandle defaultTarget) {
        return withLineNumber(delegate.switch_(input, value, keys, targets, defaultTarget));
    }

    public Terminator tryInvokeMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(delegate.tryInvokeMethod(input, owner, method, arguments, returnTarget, catchTarget));
    }

    public Terminator tryInvokeInstanceMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(delegate.tryInvokeInstanceMethod(input, instance, kind, owner, method, arguments, returnTarget, catchTarget));
    }

    public TerminatorValue tryInvokeValueMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(delegate.tryInvokeValueMethod(input, owner, method, arguments, returnTarget, catchTarget));
    }

    public TerminatorValue tryInvokeInstanceValueMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(delegate.tryInvokeInstanceValueMethod(input, instance, kind, owner, method, arguments, returnTarget, catchTarget));
    }

    public Terminator tryThrow(final MemoryState input, final Value value, final NodeHandle catchTarget) {
        return withLineNumber(delegate.tryThrow(input, value, catchTarget));
    }

    public BasicBlock block(final Terminator term) {
        return withLineNumber(delegate.block(term));
    }
}
