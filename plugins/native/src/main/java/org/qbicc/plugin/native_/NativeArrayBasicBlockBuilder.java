package org.qbicc.plugin.native_;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.NativeArrayHandle;
import org.qbicc.graph.Node;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.type.ArrayType;
import org.qbicc.type.PointerType;

public class NativeArrayBasicBlockBuilder extends DelegatingBasicBlockBuilder implements ValueHandleVisitor<Void, ValueHandle> {
    private final CompilationContext ctxt;

    public NativeArrayBasicBlockBuilder(CompilationContext context, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = context;
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        if (handle instanceof NativeArrayHandle) {
            return ((NativeArrayHandle) handle).getArrayValue();
        }
        return super.load(handle, mode);
    }

    @Override
    public ValueHandle referenceHandle(Value reference) {
        if (reference.getType() instanceof ArrayType) {
            return nativeArrayHandle(reference);
        }
        return super.referenceHandle(reference);
    }
}
