package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;

/**
 * A graph factory which sets the line number and bytecode index on each created node.
 */
public class LineNumberGraphFactory extends DelegatingGraphFactory {
    private int lineNumber; // 0 == none
    private int bytecodeIndex; // -1 == none

    public LineNumberGraphFactory(final GraphFactory delegate) {
        super(delegate);
    }

    private <N> N withLineNumber(N orig) {
        if (orig instanceof Node) {
            if (lineNumber > 0) {
                ((Node) orig).setSourceLine(lineNumber);
            }
            if (bytecodeIndex >= 0) {
                ((Node) orig).setBytecodeIndex(bytecodeIndex);
            }
        }
        return orig;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getBytecodeIndex() {
        return bytecodeIndex;
    }

    public void setBytecodeIndex(final int bytecodeIndex) {
        this.bytecodeIndex = bytecodeIndex;
    }

    public Value if_(final Context ctxt, final Value condition, final Value trueValue, final Value falseValue) {
        return withLineNumber(getDelegate().if_(ctxt, condition, trueValue, falseValue));
    }

    public Value lengthOfArray(final Context ctxt, final Value array) {
        return withLineNumber(getDelegate().lengthOfArray(ctxt, array));
    }

    public Value instanceOf(final Context ctxt, final Value v, final ClassType type) {
        return withLineNumber(getDelegate().instanceOf(ctxt, v, type));
    }

    public ParameterValue parameter(final Type type, final int index) {
        return withLineNumber(getDelegate().parameter(type, index));
    }

    public PhiValue phi(final Type type, final BasicBlock basicBlock) {
        return withLineNumber(getDelegate().phi(type, basicBlock));
    }

    public PhiValue phi(final Type type, final NodeHandle basicBlockHandle) {
        return withLineNumber(getDelegate().phi(type, basicBlockHandle));
    }

    public Value new_(final Context ctxt, final ClassType type) {
        return withLineNumber(getDelegate().new_(ctxt, type));
    }

    public Value newArray(final Context ctxt, final ArrayType type, final Value size) {
        return withLineNumber(getDelegate().newArray(ctxt, type, size));
    }

    public Value multiNewArray(final Context ctxt, final ArrayType type, final Value... dimensions) {
        return withLineNumber(getDelegate().multiNewArray(ctxt, type, dimensions));
    }

    public Value clone(final Context ctxt, final Value object) {
        return withLineNumber(getDelegate().clone(ctxt, object));
    }

    public Value pointerLoad(final Context ctxt, final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return withLineNumber(getDelegate().pointerLoad(ctxt, pointer, accessMode, atomicityMode));
    }

    public Value readInstanceField(final Context ctxt, final Value instance, final ClassType owner, final String name, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readInstanceField(ctxt, instance, owner, name, mode));
    }

    public Value readStaticField(final Context ctxt, final ClassType owner, final String name, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readStaticField(ctxt, owner, name, mode));
    }

    public Value readArrayValue(final Context ctxt, final Value array, final Value index, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readArrayValue(ctxt, array, index, mode));
    }

    public Node pointerStore(final Context ctxt, final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return withLineNumber(getDelegate().pointerStore(ctxt, pointer, value, accessMode, atomicityMode));
    }

    public Node writeInstanceField(final Context ctxt, final Value instance, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeInstanceField(ctxt, instance, owner, name, value, mode));
    }

    public Node writeStaticField(final Context ctxt, final ClassType owner, final String name, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeStaticField(ctxt, owner, name, value, mode));
    }

    public Node writeArrayValue(final Context ctxt, final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeArrayValue(ctxt, array, index, value, mode));
    }

    public Node fence(final Context ctxt, final MemoryAtomicityMode fenceType) {
        return withLineNumber(getDelegate().fence(ctxt, fenceType));
    }

    public Node monitorEnter(final Context ctxt, final Value obj) {
        return withLineNumber(getDelegate().monitorEnter(ctxt, obj));
    }

    public Node monitorExit(final Context ctxt, final Value obj) {
        return withLineNumber(getDelegate().monitorExit(ctxt, obj));
    }

    public Node invokeMethod(final Context ctxt, final ParameterizedExecutableElement target, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeMethod(ctxt, target, arguments));
    }

    public Node invokeInstanceMethod(final Context ctxt, final Value instance, final InstanceInvocation.Kind kind, final ParameterizedExecutableElement target, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeInstanceMethod(ctxt, instance, kind, target, arguments));
    }

    public Value invokeValueMethod(final Context ctxt, final MethodElement target, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeValueMethod(ctxt, target, arguments));
    }

    public Value invokeInstanceValueMethod(final Context ctxt, final Value instance, final InstanceInvocation.Kind kind, final MethodElement target, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeInstanceValueMethod(ctxt, instance, kind, target, arguments));
    }

    public BasicBlock goto_(final Context ctxt, final NodeHandle targetHandle) {
        return withLineNumber(getDelegate().goto_(ctxt, targetHandle));
    }

    public BasicBlock if_(final Context ctxt, final Value condition, final NodeHandle trueTarget, final NodeHandle falseTarget) {
        return withLineNumber(getDelegate().if_(ctxt, condition, trueTarget, falseTarget));
    }

    public BasicBlock return_(final Context ctxt) {
        return withLineNumber(getDelegate().return_(ctxt));
    }

    public BasicBlock return_(final Context ctxt, final Value value) {
        return withLineNumber(getDelegate().return_(ctxt, value));
    }

    public BasicBlock throw_(final Context ctxt, final Value value) {
        return withLineNumber(getDelegate().throw_(ctxt, value));
    }

    public BasicBlock switch_(final Context ctxt, final Value value, final int[] checkValues, final NodeHandle[] targets, final NodeHandle defaultTarget) {
        return withLineNumber(getDelegate().switch_(ctxt, value, checkValues, targets, defaultTarget));
    }

    public Value add(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().add(ctxt, v1, v2));
    }

    public Value multiply(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().multiply(ctxt, v1, v2));
    }

    public Value and(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().and(ctxt, v1, v2));
    }

    public Value or(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().or(ctxt, v1, v2));
    }

    public Value xor(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().xor(ctxt, v1, v2));
    }

    public Value cmpEq(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpEq(ctxt, v1, v2));
    }

    public Value cmpNe(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpNe(ctxt, v1, v2));
    }

    public Value shr(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().shr(ctxt, v1, v2));
    }

    public Value shl(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().shl(ctxt, v1, v2));
    }

    public Value sub(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().sub(ctxt, v1, v2));
    }

    public Value divide(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().divide(ctxt, v1, v2));
    }

    public Value remainder(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().remainder(ctxt, v1, v2));
    }

    public Value cmpLt(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpLt(ctxt, v1, v2));
    }

    public Value cmpGt(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpGt(ctxt, v1, v2));
    }

    public Value cmpLe(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpLe(ctxt, v1, v2));
    }

    public Value cmpGe(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpGe(ctxt, v1, v2));
    }

    public Value rol(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().rol(ctxt, v1, v2));
    }

    public Value ror(final Context ctxt, final Value v1, final Value v2) {
        return withLineNumber(getDelegate().ror(ctxt, v1, v2));
    }

    public Value negate(final Context ctxt, final Value v) {
        return withLineNumber(getDelegate().negate(ctxt, v));
    }

    public Value byteSwap(final Context ctxt, final Value v) {
        return withLineNumber(getDelegate().byteSwap(ctxt, v));
    }

    public Value bitReverse(final Context ctxt, final Value v) {
        return withLineNumber(getDelegate().bitReverse(ctxt, v));
    }

    public Value countLeadingZeros(final Context ctxt, final Value v) {
        return withLineNumber(getDelegate().countLeadingZeros(ctxt, v));
    }

    public Value countTrailingZeros(final Context ctxt, final Value v) {
        return withLineNumber(getDelegate().countTrailingZeros(ctxt, v));
    }

    public Value truncate(final Context ctxt, final Value value, final WordType toType) {
        return withLineNumber(getDelegate().truncate(ctxt, value, toType));
    }

    public Value extend(final Context ctxt, final Value value, final WordType toType) {
        return withLineNumber(getDelegate().extend(ctxt, value, toType));
    }

    public Value bitCast(final Context ctxt, final Value value, final WordType toType) {
        return withLineNumber(getDelegate().bitCast(ctxt, value, toType));
    }

    public Value valueConvert(final Context ctxt, final Value value, final WordType toType) {
        return withLineNumber(getDelegate().valueConvert(ctxt, value, toType));
    }

    public Value populationCount(final Context ctxt, final Value v) {
        return withLineNumber(getDelegate().populationCount(ctxt, v));
    }

    public BasicBlock jsr(final Context ctxt, final NodeHandle target, final NodeHandle ret) {
        return withLineNumber(getDelegate().jsr(ctxt, target, ret));
    }

    public BasicBlock ret(final Context ctxt, final Value address) {
        return withLineNumber(getDelegate().ret(ctxt, address));
    }
}
