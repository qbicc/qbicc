package org.qbicc.plugin.correctness;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class StaticChecksBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;
    private final ExecutableElement originalElement;

    public StaticChecksBasicBlockBuilder(CompilationContext ctxt, BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
        this.originalElement = delegate.getCurrentElement();
    }

    @Override
    public ValueHandle memberOf(ValueHandle structHandle, CompoundType.Member member) {
        if (structHandle.getValueType() instanceof CompoundType) {
            return super.memberOf(structHandle, member);
        }
        ctxt.error(getLocation(), "`memberOf` handle must have structure type");
        throw new BlockEarlyTermination(unreachable());
    }

    @Override
    public ValueHandle elementOf(ValueHandle array, Value index) {
        if (array.getValueType() instanceof ArrayType || array.getValueType() instanceof ArrayObjectType) {
            if (index.getType() instanceof UnsignedIntegerType uit) {
                // try to extend it
                Value extended = tryExtend(index, uit);
                if (extended != null) {
                    index = extended;
                } else {
                    ctxt.error(getLocation(), "`elementOf` index must be signed");
                }
                // recoverable
            }
            return super.elementOf(array, index);
        }
        ctxt.error(getLocation(), "`elementOf` handle must have array type");
        throw new BlockEarlyTermination(unreachable());
    }

    @Override
    public ValueHandle pointerHandle(Value pointer, Value offsetValue) {
        if (pointer.getType() instanceof PointerType) {
            if (offsetValue.getType() instanceof UnsignedIntegerType uit) {
                // try to extend it
                Value extended = tryExtend(offsetValue, uit);
                if (extended != null) {
                    offsetValue = extended;
                } else {
                    ctxt.error(getLocation(), "`pointerHandle` offset must be signed");
                }
                // recoverable
            }
            return super.pointerHandle(pointer, offsetValue);
        }
        ctxt.error(getLocation(), "`pointerHandle` value must have pointer type");
        throw new BlockEarlyTermination(unreachable());
    }

    @Override
    public Value bitCast(Value value, WordType toType) {
        if (value.getType() instanceof ReferenceType && toType instanceof PointerType) {
            ctxt.error(getLocation(), "Cannot bitcast references to pointers");
        } else if (value.getType() instanceof PointerType && toType instanceof ReferenceType) {
            ctxt.error(getLocation(), "Cannot bitcast pointers to references");
        } else if (value.getType().getSize() != toType.getSize()) {
            ctxt.error(getLocation(), "Cannot bitcast between differently-sized types");
        }
        return super.bitCast(value, toType);
    }

    private Value tryExtend(final Value unsignedValue, final UnsignedIntegerType inputType) {
        final BasicBlockBuilder fb = getFirstBuilder();
        final TypeSystem ts = ctxt.getTypeSystem();
        if (inputType.getMinBits() < 32) {
            return fb.extend(unsignedValue, ts.getSignedInteger32Type());
        } else if (inputType.getMinBits() < 64) {
            return fb.extend(unsignedValue, ts.getSignedInteger64Type());
        } else if (unsignedValue.isDefLe(ctxt.getLiteralFactory().literalOf(ts.getSignedInteger64Type().getMaxValue()))) {
            return fb.bitCast(unsignedValue, ts.getSignedInteger64Type());
        } else {
            // cannot work out a safe conversion
            return null;
        }
    }
}
