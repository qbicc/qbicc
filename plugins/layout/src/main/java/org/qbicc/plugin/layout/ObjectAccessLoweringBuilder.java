package org.qbicc.plugin.layout;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DecodeReference;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.InstanceFieldElement;

/**
 *
 */
public class ObjectAccessLoweringBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<Void, ValueHandle> {
    private final CompilationContext ctxt;

    public ObjectAccessLoweringBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public ValueHandle pointerHandle(Value pointer) {
        BasicBlockBuilder fb = getFirstBuilder();
        PointerType pointerType = (PointerType) pointer.getType();
        if (pointerType.getPointeeType() instanceof PhysicalObjectType pot) {
            Layout layout = Layout.get(ctxt);
            LayoutInfo info = layout.getInstanceLayoutInfo(pot.getDefinition());
            return fb.pointerHandle(fb.valueConvert(pointer, info.getCompoundType().getPointer()));
        }
        return getDelegate().pointerHandle(pointer);
    }

    @Override
    public Value valueConvert(Value value, WordType toType) {
        if (toType instanceof PointerType pt) {
            if (pt.getPointeeType() instanceof PhysicalObjectType pot) {
                BasicBlockBuilder fb = getFirstBuilder();
                Layout layout = Layout.get(ctxt);
                LayoutInfo info = layout.getInstanceLayoutInfo(pot.getDefinition());
                PointerType newType = info.getCompoundType().getPointer();
                if (value.getType() instanceof PointerType) {
                    return fb.bitCast(value, newType);
                } else {
                    return fb.valueConvert(value, newType);
                }
            }
        }
        return getDelegate().valueConvert(value, toType);
    }

    @Override
    public Value stackAllocate(ValueType type, Value count, Value align) {
        if (type instanceof PhysicalObjectType pot) {
            BasicBlockBuilder fb = getFirstBuilder();
            Layout layout = Layout.get(ctxt);
            LayoutInfo info = layout.getInstanceLayoutInfo(pot.getDefinition());
            PointerType newType = info.getCompoundType().getPointer();
            return fb.stackAllocate(newType, count, align);
        }
        return super.stackAllocate(type, count, align);
    }

    public Value elementOf(Value arrayPointer, Value index) {
        if (arrayPointer.getPointeeType() instanceof CompoundType ct && ct.getMemberCount() > 0) {
            // ElementOf a CompoundType -> ElementOf the last Member
            CompoundType.Member lastMember = ct.getMember(ct.getMemberCount() - 1);
            if (lastMember.getType() instanceof ArrayType) {
                BasicBlockBuilder fb = getFirstBuilder();
                return fb.elementOf(fb.memberOf(arrayPointer, lastMember), index);
            }
        }
        return getDelegate().elementOf(arrayPointer, index);
    }

    @Override
    public Value decodeReference(Value reference, PointerType pointerType) {
        // convert reference to pointer with lower type
        Layout layout = Layout.get(ctxt);
        ReferenceType referenceType = (ReferenceType) reference.getType();
        ObjectType upperBound = referenceType.getUpperBound();
        LayoutInfo info;
        if (upperBound instanceof ReferenceArrayObjectType raot) {
            info = layout.getArrayLayoutInfo(CoreClasses.get(ctxt).getArrayContentField(upperBound).getEnclosingType(), raot.getElementObjectType());
        } else if (upperBound instanceof ArrayObjectType) {
            info = layout.getInstanceLayoutInfo(CoreClasses.get(ctxt).getArrayContentField(upperBound).getEnclosingType());
        } else {
            info = layout.getInstanceLayoutInfo(upperBound.getDefinition());
        }
        return super.decodeReference(reference, info.getCompoundType().getPointer());
    }

    @Override
    public Value instanceFieldOf(Value instance, InstanceFieldElement field) {
        BasicBlockBuilder fb = getFirstBuilder();
        Layout layout = Layout.get(ctxt);
        LayoutInfo layoutInfo = layout.getInstanceLayoutInfo(field.getEnclosingType());
        return fb.memberOf(instance, layoutInfo.getMember(field));
    }

    @Override
    public ValueHandle unsafeHandle(ValueHandle base, Value offset, ValueType outputType) {
        BasicBlockBuilder fb = getFirstBuilder();
        UnsignedIntegerType u8 = ctxt.getTypeSystem().getUnsignedInteger8Type();
        ValueHandle valueHandle = fb.pointerHandle(fb.bitCast(fb.addressOf(base), u8.getPointer()), offset);
        return fb.pointerHandle(fb.bitCast(fb.addressOf(valueHandle), outputType.getPointer()));
    }
}
