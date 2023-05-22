package org.qbicc.plugin.layout;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.StructType;
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
public class ObjectAccessLoweringBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ObjectAccessLoweringBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value valueConvert(Value value, WordType toType) {
        if (toType instanceof PointerType pt) {
            if (pt.getPointeeType() instanceof PhysicalObjectType pot) {
                BasicBlockBuilder fb = getFirstBuilder();
                Layout layout = Layout.get(ctxt);
                LayoutInfo info = layout.getInstanceLayoutInfo(pot.getDefinition());
                PointerType newType = info.getStructType().getPointer();
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
            PointerType newType = info.getStructType().getPointer();
            return fb.stackAllocate(newType, count, align);
        }
        return super.stackAllocate(type, count, align);
    }

    public Value elementOf(Value arrayPointer, Value index) {
        if (arrayPointer.getPointeeType() instanceof StructType st && st.getMemberCount() > 0) {
            // ElementOf a StructType -> ElementOf the last Member
            StructType.Member lastMember = st.getMember(st.getMemberCount() - 1);
            if (lastMember.getType() instanceof ArrayType) {
                BasicBlockBuilder fb = getFirstBuilder();
                return fb.elementOf(fb.memberOf(arrayPointer, lastMember), index);
            }
        }
        return getDelegate().elementOf(arrayPointer, index);
    }

    @Override
    public Value decodeReference(Value reference, PointerType pointerType) {
        if (reference.getType() instanceof ReferenceType referenceType) {
            // convert reference to pointer with lower type
            Layout layout = Layout.get(ctxt);
            ObjectType upperBound = referenceType.getUpperBound();
            LayoutInfo info;
            if (upperBound instanceof ReferenceArrayObjectType raot) {
                info = layout.getArrayLayoutInfo(CoreClasses.get(ctxt).getArrayContentField(upperBound).getEnclosingType(), raot.getElementObjectType());
            } else if (upperBound instanceof ArrayObjectType) {
                info = layout.getInstanceLayoutInfo(CoreClasses.get(ctxt).getArrayContentField(upperBound).getEnclosingType());
            } else {
                info = layout.getInstanceLayoutInfo(upperBound.getDefinition());
            }
            return super.decodeReference(reference, info.getStructType().getPointer());
        } else {
            return super.decodeReference(reference, pointerType);
        }
    }

    @Override
    public Value instanceFieldOf(Value instance, InstanceFieldElement field) {
        BasicBlockBuilder fb = getFirstBuilder();
        Layout layout = Layout.get(ctxt);
        LayoutInfo layoutInfo = layout.getInstanceLayoutInfo(field.getEnclosingType());
        return fb.memberOf(instance, layoutInfo.getMember(field));
    }

    @Override
    public Value byteOffsetPointer(Value base, Value offset, ValueType outputType) {
        BasicBlockBuilder fb = getFirstBuilder();
        UnsignedIntegerType u8 = ctxt.getTypeSystem().getUnsignedInteger8Type();
        Value valueHandle = fb.offsetPointer(fb.bitCast(base, u8.getPointer()), offset);
        return fb.bitCast(valueHandle, outputType.getPointer());
    }
}
