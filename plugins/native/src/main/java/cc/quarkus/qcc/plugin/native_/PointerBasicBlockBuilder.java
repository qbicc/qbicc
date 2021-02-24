package cc.quarkus.qcc.plugin.native_;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockEarlyTermination;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ValueType;

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
    public Value narrow(Value input, ValueType toType) {
        if (input.getType() instanceof PointerType && toType instanceof ArrayType) {
            // narrowing a pointer to an array is actually an array view of a pointer
            return input;
        } else {
            return super.narrow(input, toType);
        }
    }

    @Override
    public ValueHandle referenceHandle(Value reference) {
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
