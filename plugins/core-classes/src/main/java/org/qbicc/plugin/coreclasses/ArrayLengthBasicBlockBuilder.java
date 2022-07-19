package org.qbicc.plugin.coreclasses;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.PointerValue;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ValueType;

public class ArrayLengthBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ArrayLengthBasicBlockBuilder(FactoryContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public PointerValue lengthOf(PointerValue array) {
        ValueType arrayType = array.getPointeeType();
        if (arrayType instanceof ArrayObjectType) {
            return instanceFieldOf(array, CoreClasses.get(ctxt).getArrayLengthField());
        }
        return super.lengthOf(array);
    }
}
