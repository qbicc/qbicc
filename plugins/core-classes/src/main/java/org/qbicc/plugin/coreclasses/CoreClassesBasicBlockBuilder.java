package org.qbicc.plugin.coreclasses;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.PhysicalObjectType;
import org.qbicc.type.ValueType;

public class CoreClassesBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public CoreClassesBasicBlockBuilder(FactoryContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    public Value loadLength(Value array) {
        ValueType arrayType = array.getPointeeType();
        if (arrayType instanceof ArrayObjectType) {
            return load(instanceFieldOf(array, CoreClasses.get(ctxt).getArrayLengthField()));
        }
        return super.loadLength(array);
    }

    @Override
    public Value loadTypeId(Value objectPointer) {
        ValueType objectType = objectPointer.getPointeeType();
        if (objectType instanceof PhysicalObjectType) {
            return load(instanceFieldOf(objectPointer, CoreClasses.get(ctxt).getObjectTypeIdField()));
        }
        return super.loadTypeId(objectPointer);
    }
}
