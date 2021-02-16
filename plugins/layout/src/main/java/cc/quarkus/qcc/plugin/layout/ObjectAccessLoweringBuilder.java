package cc.quarkus.qcc.plugin.layout;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.PointerHandle;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.PhysicalObjectType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.FieldElement;

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
        return super.arrayLength(arrayHandle);
    }

    @Override
    public ValueHandle referenceHandle(Value reference) {
        ValueType type = reference.getType();
        if (type instanceof ReferenceType) {
            Layout layout = Layout.get(ctxt);
            PhysicalObjectType upperBound = ((ReferenceType) type).getUpperBound();
            if (upperBound instanceof ArrayObjectType) {
                // get the appropriate content field array handle
                FieldElement contentField = layout.getArrayContentField(upperBound);
                Value pointer = getPointerFromReference(reference, layout, contentField.getEnclosingType().validate().getClassType());
                return instanceFieldOf(pointerHandle(pointer), contentField);
            } else {
                Value pointer = getPointerFromReference(reference, layout, upperBound);
                return pointerHandle(pointer);
            }
        }
        // something non-reference-ish
        return super.referenceHandle(reference);
    }

    @Override
    public ValueHandle instanceFieldOf(ValueHandle instance, FieldElement field) {
        Layout layout = Layout.get(ctxt);
        Layout.LayoutInfo info = layout.getInstanceLayoutInfo(field.getEnclosingType());
        CompoundType.Member member = info.getMember(field);
        if (instance instanceof PointerHandle) {
            // we need to recast the pointer to our type because a pointer can be covariant
            Value pointerValue = ((PointerHandle) instance).getPointerValue();
            PointerType expectedType = info.getCompoundType().getPointer();
            if (! expectedType.equals(pointerValue.getType())) {
                return memberOf(pointerHandle(bitCast(pointerValue, expectedType)), member);
            }
        }
        return memberOf(instance, member);
    }

    private Value getPointerFromReference(final Value reference, final Layout layout, final PhysicalObjectType upperBound) {
        return valueConvert(reference, layout.getInstanceLayoutInfo(upperBound.getDefinition()).getCompoundType().getPointer());
    }
}
