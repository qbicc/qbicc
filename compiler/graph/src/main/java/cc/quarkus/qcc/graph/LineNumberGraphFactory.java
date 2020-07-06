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

    public PhiMemoryState phiMemory(final BasicBlock basicBlock) {
        return withLineNumber(getDelegate().phiMemory(basicBlock));
    }

    public Value castOperation(final WordCastValue.Kind kind, final Value value, final WordType toType) {
        return withLineNumber(getDelegate().castOperation(kind, value, toType));
    }

    public MemoryState initialMemoryState() {
        return withLineNumber(getDelegate().initialMemoryState());
    }

    public MemoryStateValue new_(final MemoryState input, final ClassType type) {
        return withLineNumber(getDelegate().new_(input, type));
    }

    public MemoryStateValue newArray(final MemoryState input, final ArrayType type, final Value size) {
        return withLineNumber(getDelegate().newArray(input, type, size));
    }

    public MemoryStateValue pointerLoad(final MemoryState input, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return withLineNumber(getDelegate().pointerLoad(input, pointer, accessMode, atomicityMode));
    }

    public MemoryStateValue readInstanceField(final MemoryState input, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readInstanceField(input, instance, owner, name, mode));
    }

    public MemoryStateValue readStaticField(final MemoryState input, final ClassType owner, final String name, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readStaticField(input, owner, name, mode));
    }

    public MemoryStateValue readArrayValue(final MemoryState input, final Value array, final Value index, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readArrayValue(input, array, index, mode));
    }

    public MemoryState pointerStore(final MemoryState input, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return withLineNumber(getDelegate().pointerStore(input, pointer, value, accessMode, atomicityMode));
    }

    public MemoryState writeInstanceField(final MemoryState input, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeInstanceField(input, instance, owner, name, value, mode));
    }

    public MemoryState writeStaticField(final MemoryState input, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeStaticField(input, owner, name, value, mode));
    }

    public MemoryState writeArrayValue(final MemoryState input, final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeArrayValue(input, array, index, value, mode));
    }

    public MemoryState fence(final MemoryState input, final MemoryAtomicityMode fenceType) {
        return withLineNumber(getDelegate().fence(input, fenceType));
    }

    public MemoryState invokeMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeMethod(input, owner, method, arguments));
    }

    public MemoryState invokeInstanceMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeInstanceMethod(input, instance, kind, owner, method, arguments));
    }

    public MemoryStateValue invokeValueMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeValueMethod(input, owner, method, arguments));
    }

    public MemoryStateValue invokeInstanceValueMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeInstanceValueMethod(input, instance, kind, owner, method, arguments));
    }

    public Terminator goto_(final MemoryState input, final NodeHandle targetHandle) {
        return withLineNumber(getDelegate().goto_(input, targetHandle));
    }

    public Terminator if_(final MemoryState input, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
        return withLineNumber(getDelegate().if_(input, condition, trueTarget, falseTarget));
    }

    public Terminator return_(final MemoryState input) {
        return withLineNumber(getDelegate().return_(input));
    }

    public Terminator return_(final MemoryState input, final Value value) {
        return withLineNumber(getDelegate().return_(input, value));
    }

    public Terminator throw_(final MemoryState input, final Value value) {
        return withLineNumber(getDelegate().throw_(input, value));
    }

    public Terminator switch_(final MemoryState input, final Value value, final int[] keys, final NodeHandle[] targets, final NodeHandle defaultTarget) {
        return withLineNumber(getDelegate().switch_(input, value, keys, targets, defaultTarget));
    }

    public Terminator tryInvokeMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(getDelegate().tryInvokeMethod(input, owner, method, arguments, returnTarget, catchTarget));
    }

    public Terminator tryInvokeInstanceMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(getDelegate().tryInvokeInstanceMethod(input, instance, kind, owner, method, arguments, returnTarget, catchTarget));
    }

    public TerminatorValue tryInvokeValueMethod(final MemoryState input, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(getDelegate().tryInvokeValueMethod(input, owner, method, arguments, returnTarget, catchTarget));
    }

    public TerminatorValue tryInvokeInstanceValueMethod(final MemoryState input, final Value instance, final InstanceInvocation.Kind kind, final ClassType owner, final MethodIdentifier method, final List<Value> arguments, final NodeHandle returnTarget, final NodeHandle catchTarget) {
        return withLineNumber(getDelegate().tryInvokeInstanceValueMethod(input, instance, kind, owner, method, arguments, returnTarget, catchTarget));
    }

    public Terminator tryThrow(final MemoryState input, final Value value, final NodeHandle catchTarget) {
        return withLineNumber(getDelegate().tryThrow(input, value, catchTarget));
    }

    public BasicBlock block(final Terminator term) {
        return withLineNumber(getDelegate().block(term));
    }
}
