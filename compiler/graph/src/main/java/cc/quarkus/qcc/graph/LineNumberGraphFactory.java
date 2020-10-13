package cc.quarkus.qcc.graph;

import java.util.List;

import cc.quarkus.qcc.graph.literal.ArrayTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * A graph factory which sets the line number and bytecode index on each created node.
 */
public class LineNumberGraphFactory extends DelegatingGraphFactory {
    private int lineNumber; // 0 == none
    private int bytecodeIndex; // -1 == none

    public LineNumberGraphFactory(final BasicBlockBuilder delegate) {
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

    public Value select(final Value condition, final Value trueValue, final Value falseValue) {
        return withLineNumber(getDelegate().select(condition, trueValue, falseValue));
    }

    public Value arrayLength(final Value array) {
        return withLineNumber(getDelegate().arrayLength(array));
    }

    public Value parameter(final ValueType type, final int index) {
        return withLineNumber(getDelegate().parameter(type, index));
    }

    public PhiValue phi(final ValueType type) {
        return withLineNumber(getDelegate().phi(type));
    }

    public Value new_(final ClassTypeIdLiteral typeId) {
        return withLineNumber(getDelegate().new_(typeId));
    }

    public Value newArray(final ArrayTypeIdLiteral arrayTypeId, final Value size) {
        return withLineNumber(getDelegate().newArray(arrayTypeId, size));
    }

    public Value multiNewArray(final ArrayTypeIdLiteral arrayTypeId, final Value... dimensions) {
        return withLineNumber(getDelegate().multiNewArray(arrayTypeId, dimensions));
    }

    public Value clone(final Value object) {
        return withLineNumber(getDelegate().clone(object));
    }

    public Value pointerLoad(final Value pointer, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return withLineNumber(getDelegate().pointerLoad(pointer, accessMode, atomicityMode));
    }

    public Value readInstanceField(final Value instance, final FieldElement fieldElement, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readInstanceField(instance, fieldElement, mode));
    }

    public Value readStaticField(final FieldElement fieldElement, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readStaticField(fieldElement, mode));
    }

    public Value readArrayValue(final Value array, final Value index, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().readArrayValue(array, index, mode));
    }

    public Node pointerStore(final Value pointer, final Value value, final MemoryAccessMode accessMode, final MemoryAtomicityMode atomicityMode) {
        return withLineNumber(getDelegate().pointerStore(pointer, value, accessMode, atomicityMode));
    }

    public Node writeInstanceField(final Value instance, final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeInstanceField(instance, fieldElement, value, mode));
    }

    public Node writeStaticField(final FieldElement fieldElement, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeStaticField(fieldElement, value, mode));
    }

    public Node writeArrayValue(final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        return withLineNumber(getDelegate().writeArrayValue(array, index, value, mode));
    }

    public Node fence(final MemoryAtomicityMode fenceType) {
        return withLineNumber(getDelegate().fence(fenceType));
    }

    public Node monitorEnter(final Value obj) {
        return withLineNumber(getDelegate().monitorEnter(obj));
    }

    public Node monitorExit(final Value obj) {
        return withLineNumber(getDelegate().monitorExit(obj));
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeStatic(target, arguments));
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeInstance(kind, instance, target, arguments));
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeValueStatic(target, arguments));
    }

    public Value invokeInstanceValueMethod(final Value instance, final DispatchInvocation.Kind kind, final MethodElement target, final List<Value> arguments) {
        return withLineNumber(getDelegate().invokeInstanceValueMethod(instance, kind, target, arguments));
    }

    public Node begin(final BlockLabel blockLabel) {
        return withLineNumber(getDelegate().begin(blockLabel));
    }

    public BasicBlock goto_(final BlockLabel resumeLabel) {
        return withLineNumber(getDelegate().goto_(resumeLabel));
    }

    public Value receiver(final TypeIdLiteral upperBound) {
        return withLineNumber(getDelegate().receiver(upperBound));
    }

    public BasicBlock if_(final Value condition, final BlockLabel trueTarget, final BlockLabel falseTarget) {
        return withLineNumber(getDelegate().if_(condition, trueTarget, falseTarget));
    }

    public BasicBlock return_() {
        return withLineNumber(getDelegate().return_());
    }

    public BasicBlock return_(final Value value) {
        return withLineNumber(getDelegate().return_(value));
    }

    public BasicBlock throw_(final Value value) {
        return withLineNumber(getDelegate().throw_(value));
    }

    public BasicBlock switch_(final Value value, final int[] checkValues, final BlockLabel[] targets, final BlockLabel defaultTarget) {
        return withLineNumber(getDelegate().switch_(value, checkValues, targets, defaultTarget));
    }

    public Value add(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().add(v1, v2));
    }

    public Value multiply(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().multiply(v1, v2));
    }

    public Value and(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().and(v1, v2));
    }

    public Value or(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().or(v1, v2));
    }

    public Value xor(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().xor(v1, v2));
    }

    public Value cmpEq(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpEq(v1, v2));
    }

    public Value cmpNe(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpNe(v1, v2));
    }

    public Value shr(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().shr(v1, v2));
    }

    public Value shl(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().shl(v1, v2));
    }

    public Value sub(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().sub(v1, v2));
    }

    public Value divide(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().divide(v1, v2));
    }

    public Value remainder(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().remainder(v1, v2));
    }

    public Value cmpLt(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpLt(v1, v2));
    }

    public Value cmpGt(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpGt(v1, v2));
    }

    public Value cmpLe(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpLe(v1, v2));
    }

    public Value cmpGe(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().cmpGe(v1, v2));
    }

    public Value rol(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().rol(v1, v2));
    }

    public Value ror(final Value v1, final Value v2) {
        return withLineNumber(getDelegate().ror(v1, v2));
    }

    public Value negate(final Value v) {
        return withLineNumber(getDelegate().negate(v));
    }

    public Value byteSwap(final Value v) {
        return withLineNumber(getDelegate().byteSwap(v));
    }

    public Value bitReverse(final Value v) {
        return withLineNumber(getDelegate().bitReverse(v));
    }

    public Value countLeadingZeros(final Value v) {
        return withLineNumber(getDelegate().countLeadingZeros(v));
    }

    public Value countTrailingZeros(final Value v) {
        return withLineNumber(getDelegate().countTrailingZeros(v));
    }

    public Value truncate(final Value value, final WordType toType) {
        return withLineNumber(getDelegate().truncate(value, toType));
    }

    public Value extend(final Value value, final WordType toType) {
        return withLineNumber(getDelegate().extend(value, toType));
    }

    public Value bitCast(final Value value, final WordType toType) {
        return withLineNumber(getDelegate().bitCast(value, toType));
    }

    public Value valueConvert(final Value value, final WordType toType) {
        return withLineNumber(getDelegate().valueConvert(value, toType));
    }

    public Value populationCount(final Value v) {
        return withLineNumber(getDelegate().populationCount(v));
    }

    public BasicBlock jsr(final BlockLabel subLabel, final BlockLiteral returnAddress) {
        return withLineNumber(getDelegate().jsr(subLabel, returnAddress));
    }

    public BasicBlock ret(final Value address) {
        return withLineNumber(getDelegate().ret(address));
    }
}
