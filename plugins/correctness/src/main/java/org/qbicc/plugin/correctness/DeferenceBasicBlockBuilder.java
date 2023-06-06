package org.qbicc.plugin.correctness;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEntry;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Dereference;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.GlobalAccessMode;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.StructType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.UnionType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.InstanceMethodElement;
import org.qbicc.type.definition.element.LocalVariableElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public final class DeferenceBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    public DeferenceBasicBlockBuilder(FactoryContext fc, BasicBlockBuilder delegate) {
        super(delegate);
    }

    @Override
    public Value checkcast(Value value, Value toType, Value toDimensions, CheckCast.CastType kind, ObjectType expectedType) {
        return super.checkcast(rhs(value), rhs(toType), toDimensions, kind, expectedType);
    }

    @Override
    public Value checkcast(Value value, TypeDescriptor desc) {
        return super.checkcast(rhs(value), desc);
    }

    @Override
    public Value deref(Value pointer) {
        // weird but possible...
        return super.deref(rhs(pointer));
    }

    @Override
    public Value memberOf(Value structPointer, StructType.Member member) {
        return super.memberOf(rhs(structPointer), member);
    }

    @Override
    public Value memberOfUnion(Value unionPointer, UnionType.Member member) {
        return super.memberOfUnion(rhs(unionPointer), member);
    }

    @Override
    public Value elementOf(Value arrayPointer, Value index) {
        return super.elementOf(rhs(arrayPointer), rhs(index));
    }

    @Override
    public Value offsetPointer(Value basePointer, Value offset) {
        return super.offsetPointer(rhs(basePointer), rhs(offset));
    }

    @Override
    public Value byteOffsetPointer(Value base, Value offset, ValueType outputType) {
        return super.byteOffsetPointer(rhs(base), rhs(offset), outputType);
    }

    @Override
    public Value lookupVirtualMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.lookupVirtualMethod(rhs(reference), owner, name, descriptor);
    }

    @Override
    public Value lookupVirtualMethod(Value reference, InstanceMethodElement method) {
        return super.lookupVirtualMethod(rhs(reference), method);
    }

    @Override
    public Value lookupInterfaceMethod(Value reference, TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.lookupInterfaceMethod(rhs(reference), owner, name, descriptor);
    }

    @Override
    public Value lookupInterfaceMethod(Value reference, InstanceMethodElement method) {
        return super.lookupInterfaceMethod(rhs(reference), method);
    }

    @Override
    public Value resolveStaticMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.resolveStaticMethod(owner, name, descriptor);
    }

    @Override
    public Value resolveInstanceMethod(TypeDescriptor owner, String name, MethodDescriptor descriptor) {
        return super.resolveInstanceMethod(owner, name, descriptor);
    }

    @Override
    public Value resolveConstructor(TypeDescriptor owner, MethodDescriptor descriptor) {
        return super.resolveConstructor(owner, descriptor);
    }

    @Override
    public Value resolveStaticField(TypeDescriptor owner, String name, TypeDescriptor type) {
        return super.resolveStaticField(owner, name, type);
    }

    @Override
    public Value instanceFieldOf(Value instancePointer, InstanceFieldElement field) {
        return super.instanceFieldOf(rhs(instancePointer), field);
    }

    @Override
    public Value instanceFieldOf(Value instancePointer, TypeDescriptor owner, String name, TypeDescriptor type) {
        return super.instanceFieldOf(rhs(instancePointer), owner, name, type);
    }

    @Override
    public Value auto(Value initializer) {
        return super.auto(rhs(initializer));
    }

    @Override
    public Value stackAllocate(ValueType type, Value count, Value align) {
        return super.stackAllocate(type, rhs(count), rhs(align));
    }

    @Override
    public BlockEntry getBlockEntry() {
        return super.getBlockEntry();
    }

    @Override
    public BasicBlock getTerminatedBlock() {
        return super.getTerminatedBlock();
    }

    @Override
    public Value offsetOfField(FieldElement fieldElement) {
        return super.offsetOfField(fieldElement);
    }

    @Override
    public Value extractElement(Value array, Value index) {
        return super.extractElement(rhs(array), rhs(index));
    }

    @Override
    public Value extractMember(Value compound, StructType.Member member) {
        return super.extractMember(rhs(compound), member);
    }

    @Override
    public Value extractInstanceField(Value valueObj, TypeDescriptor owner, String name, TypeDescriptor type) {
        return super.extractInstanceField(rhs(valueObj), owner, name, type);
    }

    @Override
    public Value extractInstanceField(Value valueObj, FieldElement field) {
        return super.extractInstanceField(rhs(valueObj), field);
    }

    @Override
    public Value insertElement(Value array, Value index, Value value) {
        return super.insertElement(rhs(array), rhs(index), rhs(value));
    }

    @Override
    public Value insertMember(Value compound, StructType.Member member, Value value) {
        return super.insertMember(rhs(compound), member, rhs(value));
    }

    @Override
    public Node declareDebugAddress(LocalVariableElement variable, Value address) {
        return super.declareDebugAddress(variable, rhs(address));
    }

    @Override
    public Node setDebugValue(LocalVariableElement variable, Value value) {
        return super.setDebugValue(variable, rhs(value));
    }

    @Override
    public Value select(Value condition, Value trueValue, Value falseValue) {
        return super.select(rhs(condition), rhs(trueValue), rhs(falseValue));
    }

    @Override
    public Value loadLength(Value arrayPointer) {
        return super.loadLength(rhs(arrayPointer));
    }

    @Override
    public Value loadTypeId(Value objectPointer) {
        return super.loadTypeId(rhs(objectPointer));
    }

    @Override
    public Value new_(ClassObjectType type, Value typeId, Value size, Value align) {
        return super.new_(type, rhs(typeId), rhs(size), rhs(align));
    }

    @Override
    public Value new_(ClassTypeDescriptor desc) {
        return super.new_(desc);
    }

    @Override
    public Value newArray(PrimitiveArrayObjectType arrayType, Value size) {
        return super.newArray(arrayType, rhs(size));
    }

    @Override
    public Value newArray(ArrayTypeDescriptor desc, Value size) {
        return super.newArray(desc, rhs(size));
    }

    @Override
    public Value newReferenceArray(ReferenceArrayObjectType arrayType, Value elemTypeId, Value dimensions, Value size) {
        return super.newReferenceArray(arrayType, rhs(elemTypeId), rhs(dimensions), rhs(size));
    }

    @Override
    public Value multiNewArray(ArrayObjectType arrayType, List<Value> dimensions) {
        return super.multiNewArray(arrayType, rhs(dimensions));
    }

    @Override
    public Value multiNewArray(ArrayTypeDescriptor desc, List<Value> dimensions) {
        return super.multiNewArray(desc, rhs(dimensions));
    }

    @Override
    public Value load(Value pointer, ReadAccessMode accessMode) {
        return super.load(rhs(pointer), accessMode);
    }

    @Override
    public Value readModifyWrite(Value pointer, ReadModifyWrite.Op op, Value update, ReadAccessMode readMode, WriteAccessMode writeMode) {
        return super.readModifyWrite(rhs(pointer), op, rhs(update), readMode, writeMode);
    }

    @Override
    public Value cmpAndSwap(Value target, Value expect, Value update, ReadAccessMode readMode, WriteAccessMode writeMode, CmpAndSwap.Strength strength) {
        return super.cmpAndSwap(rhs(target), rhs(expect), rhs(update), readMode, writeMode, strength);
    }

    @Override
    public Node store(Value handle, Value value, WriteAccessMode accessMode) {
        return super.store(rhs(handle), rhs(value), accessMode);
    }

    @Override
    public Node initCheck(InitializerElement initializer, Value initThunk) {
        return super.initCheck(initializer, rhs(initThunk));
    }

    @Override
    public Node fence(GlobalAccessMode fenceType) {
        return super.fence(fenceType);
    }

    @Override
    public Node monitorEnter(Value obj) {
        return super.monitorEnter(rhs(obj));
    }


    @Override
    public Node monitorExit(Value obj) {
        return super.monitorExit(rhs(obj));
    }

    @Override
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.call(rhs(targetPtr), rhs(receiver), rhs(arguments));
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoSideEffects(rhs(targetPtr), rhs(receiver), rhs(arguments));
    }

    @Override
    public Node begin(BlockLabel blockLabel) {
        return super.begin(blockLabel);
    }

    @Override
    public <T> BasicBlock begin(BlockLabel blockLabel, T arg, BiConsumer<T, BasicBlockBuilder> maker) {
        return super.begin(blockLabel, arg, maker);
    }

    @Override
    public Node reachable(Value value) {
        return super.reachable(rhs(value));
    }

    @Override
    public Node safePoint() {
        return super.safePoint();
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.callNoReturn(rhs(targetPtr), rhs(receiver), rhs(arguments));
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        return super.invokeNoReturn(rhs(targetPtr), rhs(receiver), rhs(arguments), catchLabel, rhs(targetArguments));
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        return super.tailCall(rhs(targetPtr), rhs(receiver), rhs(arguments));
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        return super.invoke(rhs(targetPtr), rhs(receiver), rhs(arguments), catchLabel, resumeLabel, rhs(targetArguments));
    }

    @Override
    public BasicBlock goto_(BlockLabel resumeLabel, Map<Slot, Value> args) {
        return super.goto_(resumeLabel, rhs(args));
    }

    @Override
    public BasicBlock if_(Value condition, BlockLabel trueTarget, BlockLabel falseTarget, Map<Slot, Value> targetArguments) {
        return super.if_(rhs(condition), trueTarget, falseTarget, rhs(targetArguments));
    }

    @Override
    public BasicBlock return_(Value value) {
        return super.return_(rhs(value));
    }

    @Override
    public BasicBlock unreachable() {
        return super.unreachable();
    }

    @Override
    public BasicBlock throw_(Value value) {
        return super.throw_(rhs(value));
    }

    @Override
    public BasicBlock switch_(Value value, int[] checkValues, BlockLabel[] targets, BlockLabel defaultTarget, Map<Slot, Value> targetArguments) {
        return super.switch_(rhs(value), checkValues, targets, defaultTarget, rhs(targetArguments));
    }

    @Override
    public Value add(Value v1, Value v2) {
        return super.add(rhs(v1), rhs(v2));
    }

    @Override
    public Value multiply(Value v1, Value v2) {
        return super.multiply(rhs(v1), rhs(v2));
    }

    @Override
    public Value and(Value v1, Value v2) {
        return super.and(rhs(v1), rhs(v2));
    }

    @Override
    public Value or(Value v1, Value v2) {
        return super.or(rhs(v1), rhs(v2));
    }

    @Override
    public Value xor(Value v1, Value v2) {
        return super.xor(rhs(v1), rhs(v2));
    }

    @Override
    public Value isEq(Value v1, Value v2) {
        return super.isEq(rhs(v1), rhs(v2));
    }

    @Override
    public Value isNe(Value v1, Value v2) {
        return super.isNe(rhs(v1), rhs(v2));
    }

    @Override
    public Value shr(Value v1, Value v2) {
        return super.shr(rhs(v1), rhs(v2));
    }

    @Override
    public Value shl(Value v1, Value v2) {
        return super.shl(rhs(v1), rhs(v2));
    }

    @Override
    public Value sub(Value v1, Value v2) {
        return super.sub(rhs(v1), rhs(v2));
    }

    @Override
    public Value divide(Value v1, Value v2) {
        return super.divide(rhs(v1), rhs(v2));
    }

    @Override
    public Value remainder(Value v1, Value v2) {
        return super.remainder(rhs(v1), rhs(v2));
    }

    @Override
    public Value min(Value v1, Value v2) {
        return super.min(rhs(v1), rhs(v2));
    }

    @Override
    public Value max(Value v1, Value v2) {
        return super.max(rhs(v1), rhs(v2));
    }

    @Override
    public Value isLt(Value v1, Value v2) {
        return super.isLt(rhs(v1), rhs(v2));
    }

    @Override
    public Value isGt(Value v1, Value v2) {
        return super.isGt(rhs(v1), rhs(v2));
    }

    @Override
    public Value isLe(Value v1, Value v2) {
        return super.isLe(rhs(v1), rhs(v2));
    }

    @Override
    public Value isGe(Value v1, Value v2) {
        return super.isGe(rhs(v1), rhs(v2));
    }

    @Override
    public Value rol(Value v1, Value v2) {
        return super.rol(rhs(v1), rhs(v2));
    }

    @Override
    public Value ror(Value v1, Value v2) {
        return super.ror(rhs(v1), rhs(v2));
    }

    @Override
    public Value cmp(Value v1, Value v2) {
        return super.cmp(rhs(v1), rhs(v2));
    }

    @Override
    public Value cmpG(Value v1, Value v2) {
        return super.cmpG(rhs(v1), rhs(v2));
    }

    @Override
    public Value cmpL(Value v1, Value v2) {
        return super.cmpL(rhs(v1), rhs(v2));
    }

    @Override
    public Value notNull(Value v) {
        return super.notNull(rhs(v));
    }

    @Override
    public Value negate(Value v) {
        return super.negate(rhs(v));
    }

    @Override
    public Value complement(Value v) {
        return super.complement(rhs(v));
    }

    @Override
    public Value byteSwap(Value v) {
        return super.byteSwap(rhs(v));
    }

    @Override
    public Value bitReverse(Value v) {
        return super.bitReverse(rhs(v));
    }

    @Override
    public Value countLeadingZeros(Value v) {
        return super.countLeadingZeros(rhs(v));
    }

    @Override
    public Value countTrailingZeros(Value v) {
        return super.countTrailingZeros(rhs(v));
    }

    @Override
    public Value truncate(Value value, WordType toType) {
        return super.truncate(rhs(value), toType);
    }

    @Override
    public Value extend(Value value, WordType toType) {
        return super.extend(rhs(value), toType);
    }

    @Override
    public Value bitCast(Value value, WordType toType) {
        return super.bitCast(rhs(value), toType);
    }

    @Override
    public Value fpToInt(Value value, IntegerType toType) {
        return super.fpToInt(rhs(value), toType);
    }

    @Override
    public Value intToFp(Value value, FloatType toType) {
        return super.intToFp(rhs(value), toType);
    }

    @Override
    public Value valueConvert(Value value, WordType toType) {
        return super.valueConvert(rhs(value), toType);
    }

    @Override
    public Value decodeReference(Value refVal, PointerType pointerType) {
        return super.decodeReference(rhs(refVal), pointerType);
    }

    @Override
    public Value instanceOf(Value input, ObjectType expectedType, int expectedDimensions) {
        return super.instanceOf(rhs(input), expectedType, expectedDimensions);
    }

    @Override
    public Value instanceOf(Value input, TypeDescriptor desc) {
        return super.instanceOf(rhs(input), desc);
    }

    @Override
    public Value populationCount(Value v) {
        return super.populationCount(rhs(v));
    }

    @Override
    public BasicBlock ret(Value address, Map<Slot, Value> targetArguments) {
        return super.ret(rhs(address), rhs(targetArguments));
    }

    @Override
    public Value classOf(Value typeId, Value dimensions) {
        return super.classOf(rhs(typeId), rhs(dimensions));
    }

    @Override
    public Value vaArg(Value vaList, ValueType type) {
        return super.vaArg(rhs(vaList), type);
    }

    private Value rhs(final Value val) {
        if (val instanceof Dereference d) {
            return getFirstBuilder().load(d.getPointer(), SingleUnshared);
        } else {
            return val;
        }
    }

    private List<Value> rhs(final List<Value> vals) {
        for (Value val : vals) {
            if (val instanceof Dereference) {
                List<Value> newVals = new ArrayList<>();
                for (Value value : vals) {
                    newVals.add(rhs(value));
                }
                return newVals;
            }
        }
        return vals;
    }

    private Map<Slot, Value> rhs(final Map<Slot, Value> vals) {
        for (Map.Entry<Slot, Value> entry : vals.entrySet()) {
            if (entry.getValue() instanceof Dereference) {
                Map<Slot, Value> newVals = new HashMap<>(vals);
                for (Map.Entry<Slot, Value> innerEntry : newVals.entrySet()) {
                    innerEntry.setValue(rhs(innerEntry.getValue()));
                }
                return Map.copyOf(newVals);
            }
        }
        return vals;
    }
}
