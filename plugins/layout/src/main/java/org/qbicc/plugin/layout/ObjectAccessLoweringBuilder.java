package org.qbicc.plugin.layout;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.PointerValue;
import org.qbicc.graph.PointerValueVisitor;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public class ObjectAccessLoweringBuilder extends DelegatingBasicBlockBuilder implements PointerValueVisitor<Void, PointerValue> {
    private final CompilationContext ctxt;

    public ObjectAccessLoweringBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public PointerValue offsetPointer(Value pointer, Value offsetValue) {
        BasicBlockBuilder fb = getFirstBuilder();
        PointerType pointerType = (PointerType) pointer.getType();
        if (pointerType.getPointeeType() instanceof PhysicalObjectType pot) {
            Layout layout = Layout.get(ctxt);
            LayoutInfo info = layout.getInstanceLayoutInfo(pot.getDefinition());
            return fb.pointerValueOf(fb.valueConvert(pointer, info.getCompoundType().getPointer()));
        }
        return getDelegate().offsetPointer(pointer, offsetValue);
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

    public PointerValue elementOf(PointerValue array, Value index) {
        if (array.getPointeeType() instanceof CompoundType ct && ct.getMemberCount() > 0) {
            // ElementOf a CompoundType -> ElementOf the last Member
            CompoundType.Member lastMember = ct.getMember(ct.getMemberCount() - 1);
            BasicBlockBuilder fb = getFirstBuilder();
            return fb.elementOf(fb.memberOf(array, lastMember), index);
        }
        return getDelegate().elementOf(array, index);
    }

    @Override
    public PointerValue decodeReference(Value reference) {
        BasicBlockBuilder fb = getFirstBuilder();
        // convert reference to pointer
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
        return fb.pointerValueOf(fb.valueConvert(reference, info.getCompoundType().getPointer()));
    }

    @Override
    public PointerValue instanceFieldOf(PointerValue instance, FieldElement field) {
        BasicBlockBuilder fb = getFirstBuilder();
        Layout layout = Layout.get(ctxt);
        LayoutInfo layoutInfo = layout.getInstanceLayoutInfo(field.getEnclosingType());
        return fb.memberOf(instance, layoutInfo.getMember(field));
    }

    @Override
    public PointerValue unsafeHandle(PointerValue base, Value offset, ValueType outputType) {
        BasicBlockBuilder fb = getFirstBuilder();
        UnsignedIntegerType u8 = ctxt.getTypeSystem().getUnsignedInteger8Type();
        PointerValue pointerValue = fb.offsetPointer(fb.bitCast(fb.addressOf(base), u8.getPointer()), offset);
        return fb.pointerValueOf(fb.bitCast(fb.addressOf(pointerValue), outputType.getPointer()));
    }
}
