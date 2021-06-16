package org.qbicc.plugin.native_;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Load;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.ArrayType;
import org.qbicc.type.PointerType;

/**
 * A basic block builder which handles pointer type manipulations.
 */
public class PointerBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public PointerBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public ValueHandle referenceHandle(Value reference) {
        if (reference instanceof Load) {
            ValueHandle target = reference.getValueHandle();

        }
        if (reference.getType() instanceof PointerType) {
            return pointerHandle(reference);
        } else if (reference.getType() instanceof ArrayType) {
            ctxt.error(getLocation(), "Cannot directly reference an array");
            throw new BlockEarlyTermination(unreachable());
        } else {
            return super.referenceHandle(reference);
        }
    }
}
