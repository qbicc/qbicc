package org.qbicc.plugin.layout;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
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
        return super.load(transform(handle), mode);
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        return super.store(transform(handle), value, mode);
    }

    @Override
    public Value getAndAdd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndAdd(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseAnd(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndBitwiseAnd(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseNand(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndBitwiseNand(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseOr(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndBitwiseOr(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndBitwiseXor(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndBitwiseXor(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndSet(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndSet(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndSetMax(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndSetMax(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndSetMin(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndSetMin(transform(target), update, atomicityMode);
    }

    @Override
    public Value getAndSub(ValueHandle target, Value update, MemoryAtomicityMode atomicityMode) {
        return super.getAndSub(transform(target), update, atomicityMode);
    }

    @Override
    public Value cmpAndSwap(ValueHandle target, Value expect, Value update, MemoryAtomicityMode successMode, MemoryAtomicityMode failureMode) {
        return super.cmpAndSwap(transform(target), expect, update, successMode, failureMode);
    }

    @Override
    public Value addressOf(ValueHandle handle) {
        return super.addressOf(transform(handle));
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
                            return b.transform(b.pointerHandle(b.bitCast(addr, elementType.getPointer())));
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
                return b.pointerHandle(b.valueConvert(node.getReferenceValue(), info.getCompoundType().getPointer()));
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
