package org.qbicc.plugin.coreclasses;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ValueType;

public class ArrayLengthBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ArrayLengthBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
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
}
