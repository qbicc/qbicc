package org.qbicc.plugin.layout;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public class ObjectAccessLoweringBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ObjectAccessLoweringBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value typeIdOf(final ValueHandle valueHandle) {
        Layout layout = Layout.get(ctxt);
        return load(instanceFieldOf(valueHandle, layout.getObjectTypeIdField()), MemoryAtomicityMode.UNORDERED);
    }

    public Value arrayLength(final ValueHandle arrayHandle) {
        ValueType arrayType = arrayHandle.getValueType();
        if (arrayType instanceof ArrayObjectType) {
            Layout layout = Layout.get(ctxt);
            return load(instanceFieldOf(arrayHandle, layout.getArrayLengthField()), MemoryAtomicityMode.UNORDERED);
        }
        // something non-reference-ish
        return super.arrayLength(transform(arrayHandle));
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        return intToBool(handle, super.load(transform(handle), mode));
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        return super.store(transform(handle), boolToInt(handle, value), mode);
    }

    @Override
    public Value getAndAdd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        // cannot be a boolean field
        return super.getAndAdd(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseAnd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return intToBool(target, super.getAndBitwiseAnd(transform(target), boolToInt(target, update), atomicityMode));
    }

    @Override
    public Value getAndBitwiseNand(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return intToBool(target, super.getAndBitwiseNand(transform(target), boolToInt(target, update), atomicityMode));
    }

    @Override
    public Value getAndBitwiseOr(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return intToBool(target, super.getAndBitwiseOr(transform(target), boolToInt(target, update), atomicityMode));
    }

    @Override
    public Value getAndBitwiseXor(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return intToBool(target, super.getAndBitwiseXor(transform(target), boolToInt(target, update), atomicityMode));
    }

    @Override
    public Value getAndSet(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return intToBool(target, super.getAndSet(transform(target), boolToInt(target, update), atomicityMode));
    }

    @Override
    public Value getAndSetMax(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        // cannot be a boolean field
        return super.getAndSetMax(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndSetMin(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        // cannot be a boolean field
        return super.getAndSetMin(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndSub(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        // cannot be a boolean field
        return super.getAndSub(transform(target), update, atomicityMode);
    }

    @Override
    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode) {
        Value result = super.cmpAndSwap(transform(target), boolToInt(target, expect), boolToInt(target, update), successMode, failureMode);
        // the result is a { i8, i1 } if the field is boolean
        if (target.getValueType() instanceof BooleanType) {
            // we need to change to a { i1, i1 }
            TypeSystem ts = ctxt.getTypeSystem();
            LiteralFactory lf = ctxt.getLiteralFactory();
            UnsignedIntegerType u8 = ts.getUnsignedInteger8Type();
            BooleanType bool = ts.getBooleanType();
            CompoundType origType = CmpAndSwap.getResultType(ctxt, u8);
            CompoundType newType = CmpAndSwap.getResultType(ctxt, bool);
            Value resultByteVal = extractMember(result, origType.getMember(0));
            Value resultFlag = extractMember(result, origType.getMember(1));
            result = insertMember(lf.zeroInitializerLiteralOfType(newType), newType.getMember(0), truncate(resultByteVal, bool));
            result = insertMember(result, newType.getMember(1), resultFlag);
        }
        return result;
    }

    @Override
    public Value addressOf(ValueHandle handle) {
        return super.addressOf(transform(handle));
    }

    private Value boolToInt(final ValueHandle handle, final Value value) {
        if (handle.getValueType() instanceof BooleanType) {
            // we have to widen the value to an integer
            return extend(value, ctxt.getTypeSystem().getUnsignedInteger8Type());
        }
        return value;
    }

    private Value intToBool(final ValueHandle handle, final Value value) {
        ValueType valueType = handle.getValueType();
        if (valueType instanceof BooleanType) {
            // narrow it back
            return truncate(value, (WordType) valueType);
        }
        return value;
    }

    private ValueHandle transform(ValueHandle input) {
        return input.accept(new ValueHandleVisitor<>() {
            @Override
            public ValueHandle visit(ObjectAccessLoweringBuilder b, ElementOf node) {
                ValueHandle inputHandle = node.getValueHandle();
                if (inputHandle instanceof ReferenceHandle) {
                    // Transform array object element handles
                    Layout layout = Layout.get(ctxt);
                    ObjectType upperBound = (ObjectType) inputHandle.getValueType();
                    if (upperBound instanceof ArrayObjectType) {
                        FieldElement contentField = layout.getArrayContentField(upperBound);
                        if (upperBound instanceof ReferenceArrayObjectType) {
                            ValueHandle elementHandle = b.elementOf(b.transform(b.instanceFieldOf(inputHandle, contentField)), node.getIndex());
                            Value addr = b.addressOf(elementHandle);
                            ReferenceType elementType = ((ReferenceArrayObjectType) upperBound).getElementType();
                            return b.transform(b.pointerHandle(b.bitCast(addr, elementType.getPointer().asCollected())));
                        } else {
                            assert upperBound instanceof PrimitiveArrayObjectType;
                            return b.elementOf(b.transform(b.instanceFieldOf(inputHandle, contentField)), node.getIndex());
                        }
                    }
                }
                // normal array, probably
                return b.elementOf(b.transform(inputHandle), node.getIndex());
            }

            @Override
            public ValueHandle visit(ObjectAccessLoweringBuilder b, ReferenceHandle node) {
                // convert reference to pointer
                Layout layout = Layout.get(ctxt);
                ObjectType upperBound = node.getValueType();
                Layout.LayoutInfo info;
                if (upperBound instanceof ArrayObjectType) {
                    info = layout.getInstanceLayoutInfo(layout.getArrayContentField(upperBound).getEnclosingType());
                } else {
                    info = layout.getInstanceLayoutInfo(upperBound.getDefinition());
                }
                return b.pointerHandle(b.valueConvert(node.getReferenceValue(), info.getCompoundType().getPointer().asCollected()));
            }

            @Override
            public ValueHandle visit(ObjectAccessLoweringBuilder b, InstanceFieldOf node) {
                Layout layout = Layout.get(ctxt);
                FieldElement element = node.getVariableElement();
                return b.memberOf(b.transform(node.getValueHandle()), layout.getInstanceLayoutInfo(element.getEnclosingType()).getMember(element));
            }

            @Override
            public ValueHandle visitUnknown(ObjectAccessLoweringBuilder b, ValueHandle node) {
                // all other handles are fine as-is
                return node;
            }
        }, this);
    }
}
