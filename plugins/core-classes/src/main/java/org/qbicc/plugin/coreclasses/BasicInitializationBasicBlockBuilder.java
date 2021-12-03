package org.qbicc.plugin.coreclasses;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.FieldElement;

/**
 * A basic block builder which sets up the core classes fields of object instances, which are the same regardless of heap
 * or GC implementation.
 */
public class BasicInitializationBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public BasicInitializationBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public ValueHandle lengthOf(ValueHandle array) {
        ValueType arrayType = array.getValueType();
        if (arrayType instanceof ArrayObjectType) {
            return instanceFieldOf(array, CoreClasses.get(ctxt).getArrayLengthField());
        }
        return super.lengthOf(array);
    }

    @Override
    public Value new_(ClassObjectType type) {
        Value allocated = super.new_(type);
        initializeObjectHeader(CoreClasses.get(ctxt), referenceHandle(allocated), ctxt.getLiteralFactory().literalOfType(type));
        return allocated;
    }

    @Override
    public Value newArray(PrimitiveArrayObjectType arrayType, Value size) {
        Value allocated = super.newArray(arrayType, size);
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        ClassObjectType actualType = coreClasses.getArrayContentField(arrayType).getEnclosingType().load().getClassType();
        initializeArrayHeader(coreClasses, referenceHandle(allocated), ctxt.getLiteralFactory().literalOfType(actualType), size);
        return allocated;
    }

    @Override
    public Value newReferenceArray(ReferenceArrayObjectType arrayType, Value elemTypeId, Value dimensions, Value size) {
        Value allocated = super.newReferenceArray(arrayType, elemTypeId, dimensions, size);
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        LiteralFactory lf = ctxt.getLiteralFactory();
        initializeRefArrayHeader(coreClasses, referenceHandle(allocated), elemTypeId, dimensions, size);
        return allocated;
    }

    private void initializeObjectHeader(final CoreClasses coreClasses, final ValueHandle handle, final Value typeId) {
        store(instanceFieldOf(handle, coreClasses.getObjectTypeIdField()), typeId, MemoryAtomicityMode.UNORDERED);
        FieldElement monitorField = coreClasses.getObjectNativeObjectMonitorField();
        store(instanceFieldOf(handle, monitorField), ctxt.getLiteralFactory().literalOf((IntegerType)monitorField.getType(), 0L), MemoryAtomicityMode.NONE);
    }

    private void initializeArrayHeader(final CoreClasses coreClasses, final ValueHandle handle, final Value typeId, final Value size) {
        initializeObjectHeader(coreClasses, handle, typeId);
        store(instanceFieldOf(handle, coreClasses.getArrayLengthField()), size, MemoryAtomicityMode.UNORDERED);
    }

    private void initializeRefArrayHeader(final CoreClasses coreClasses, final ValueHandle handle, Value elemTypeId, Value dimensions, final Value size) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        initializeArrayHeader(coreClasses, handle, lf.literalOfType(coreClasses.getReferenceArrayTypeDefinition().load().getClassType()), size);
        FieldElement dimsField = coreClasses.getRefArrayDimensionsField();
        store(instanceFieldOf(handle, dimsField), dimensions, MemoryAtomicityMode.UNORDERED);
        store(instanceFieldOf(handle, coreClasses.getRefArrayElementTypeIdField()), elemTypeId, MemoryAtomicityMode.UNORDERED);
    }
}
