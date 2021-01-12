package cc.quarkus.qcc.plugin.correctness;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.*;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import io.smallrye.common.constraint.Assert;

/**
 * This builder masks shift distances to ensure they match the word width of the target architecture.
 */
public class ShiftDistanceMaskingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ShiftDistanceMaskingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value shl(Value v1, Value v2) {
        return super.shl(v1, getMaskedShiftDistance(v1, v2));
    }

    @Override
    public Value shr(Value v1, Value v2) {
        return super.shr(v1, getMaskedShiftDistance(v1, v2));
    }

    private Value getMaskedShiftDistance(Value v1, Value v2) {
        final SignedIntegerType signedInteger32Type = ctxt.getTypeSystem().getSignedInteger32Type();
        final SignedIntegerType signedInteger64Type = ctxt.getTypeSystem().getSignedInteger64Type();

        final ValueType v1Type = v1.getType();
        Assert.assertTrue(v1Type == signedInteger32Type || v1Type == signedInteger64Type);

        final LiteralFactory lf = ctxt.getLiteralFactory();
        final long bits = v1.getType().getSize() * ctxt.getTypeSystem().getByteBits();
        Value v2Masked = and(v2, lf.literalOf(bits - 1));
        if (v1Type == signedInteger64Type) {
            v2Masked = extend(v2, signedInteger64Type);
        }
        return v2Masked;
    }

}
