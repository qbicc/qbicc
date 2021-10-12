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
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
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
    public Value new_(ClassObjectType type) {
        Value allocated = super.new_(type);
        ValueType allocatedType = allocated.getType();
        if (allocatedType instanceof ReferenceType) {
            initializeBaseHeader(type, allocated);
        }
        return allocated;
    }

    @Override
    public Value newArray(ArrayObjectType arrayType, Value size) {
        Value allocated = super.newArray(arrayType, size);
        ValueType allocatedType = allocated.getType();
        if (allocatedType instanceof ReferenceType) {
            initializeArrayHeader(arrayType, allocated, size);
        }
        return allocated;
    }

    private ValueHandle initializeBaseHeader(final ClassObjectType type, final Value allocated) {
        LiteralFactory lf = ctxt.getLiteralFactory();
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        ValueHandle handle = referenceHandle(allocated);
        store(instanceFieldOf(handle, coreClasses.getObjectTypeIdField()), lf.literalOfType(type), MemoryAtomicityMode.UNORDERED);
        FieldElement monitorField = coreClasses.getObjectNativeObjectMonitorField();
        store(instanceFieldOf(handle, monitorField), ctxt.getLiteralFactory().literalOf((IntegerType)monitorField.getType(), 0L), MemoryAtomicityMode.NONE);
        return handle;
    }

    private void initializeArrayHeader(final ArrayObjectType arrayType, final Value allocated, final Value size) {
        CoreClasses coreClasses = CoreClasses.get(ctxt);
        ClassObjectType actualType = coreClasses.getArrayContentField(arrayType).getEnclosingType().load().getClassType();
        ValueHandle handle = initializeBaseHeader(actualType, allocated);
        store(instanceFieldOf(handle, coreClasses.getArrayLengthField()), size, MemoryAtomicityMode.UNORDERED);
        LiteralFactory lf = ctxt.getLiteralFactory();
        if (arrayType instanceof ReferenceArrayObjectType) {
            // we also have to store the element type and dimensions
            ReferenceArrayObjectType raot = (ReferenceArrayObjectType) arrayType;
            int dimensionCount = raot.getDimensionCount();
            ObjectType leafType = raot.getLeafElementType();
            FieldElement dimsField = coreClasses.getRefArrayDimensionsField();
            store(instanceFieldOf(handle, dimsField), lf.literalOf((IntegerType) dimsField.getType(), dimensionCount), MemoryAtomicityMode.UNORDERED);
            store(instanceFieldOf(handle, coreClasses.getRefArrayElementTypeIdField()), lf.literalOfType(leafType), MemoryAtomicityMode.UNORDERED);
        }
    }
}
