package cc.quarkus.qcc.plugin.conversion;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.FloatLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;

/**
 * This builder fixes up mismatched numerical conversions in order to avoid duplicating this kind of logic in other
 * builders.
 */
public class NumericalConversionBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NumericalConversionBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value truncate(final Value from, final WordType toType) {
        ValueType fromTypeRaw = from.getType();
        if (fromTypeRaw instanceof WordType) {
            WordType fromType = (WordType) fromTypeRaw;
            if (fromType instanceof SignedIntegerType) {
                if (toType instanceof SignedIntegerType) {
                    if (fromType.getMinBits() > toType.getMinBits()) {
                        // OK
                        return super.truncate(from, toType);
                    } else if (fromType.getMinBits() == toType.getMinBits()) {
                        // no actual truncation needed
                        return from;
                    } else {
                        assert fromType.getMinBits() < toType.getMinBits();
                        // actually an extension; not OK (fall out)
                    }
                } else if (toType instanceof UnsignedIntegerType) {
                    // OK in general but needs to be converted first
                    return truncate(bitCast(from, ((SignedIntegerType) fromType).asUnsigned()), toType);
                }
                // otherwise not OK (fall out)
            } else if (fromType instanceof UnsignedIntegerType) {
                if (toType instanceof UnsignedIntegerType) {
                    if (fromType.getMinBits() > toType.getMinBits()) {
                        // OK
                        return super.truncate(from, toType);
                    } else if (fromType.getMinBits() == toType.getMinBits()) {
                        // no actual truncation needed
                        return from;
                    } else {
                        assert fromType.getMinBits() < toType.getMinBits();
                        // actually an extension; not OK (fall out)
                    }
                } else if (toType instanceof SignedIntegerType) {
                    // OK in general but needs to be converted first
                    return truncate(bitCast(from, ((UnsignedIntegerType) fromType).asSigned()), toType);
                }
                // otherwise not OK (fall out)
            } else if (fromType instanceof FloatType) {
                if (toType instanceof FloatType) {
                    if (fromType.getMinBits() > toType.getMinBits()) {
                        // OK
                        return super.truncate(from, toType);
                    } else if (fromType.getMinBits() == toType.getMinBits()) {
                        // no actual truncation needed
                        return from;
                    } else {
                        assert fromType.getMinBits() < toType.getMinBits();
                        // actually an extension; not OK (fall out)
                    }
                }
            }
        }
        // report the error but produce the node anyway and continue
        ctxt.error(getLocation(), "Invalid truncation of %s to %s", fromTypeRaw, toType);
        return super.truncate(from, toType);
    }

    public Value extend(final Value from, final WordType toType) {
        ValueType fromTypeRaw = from.getType();
        if (fromTypeRaw instanceof WordType) {
            WordType fromType = (WordType) fromTypeRaw;
            if (fromType instanceof SignedIntegerType) {
                if (toType instanceof SignedIntegerType) {
                    if (fromType.getMinBits() < toType.getMinBits()) {
                        // OK
                        return super.extend(from, toType);
                    } else if (fromType.getMinBits() == toType.getMinBits()) {
                        // no actual extension needed
                        return from;
                    } else {
                        assert fromType.getMinBits() > toType.getMinBits();
                        // actually a truncation; not OK (fall out)
                    }
                } else if (toType instanceof UnsignedIntegerType) {
                    // not OK specifically
                    ctxt.error(getLocation(),
                        "Cannot extend a signed integer of type %s into a wider unsigned integer of type %s:"
                            + " either sign-extend to %s first or cast to %s first",
                        fromType, toType, ((UnsignedIntegerType) toType).asSigned(), ((SignedIntegerType) fromType).asUnsigned());
                    return super.extend(from, toType);
                }
                // otherwise not OK (fall out)
            } else if (fromType instanceof UnsignedIntegerType) {
                if (toType instanceof UnsignedIntegerType) {
                    if (fromType.getMinBits() < toType.getMinBits()) {
                        // OK
                        return super.extend(from, toType);
                    } else if (fromType.getMinBits() == toType.getMinBits()) {
                        // no actual extension needed
                        return from;
                    } else {
                        assert fromType.getMinBits() < toType.getMinBits();
                        // actually a truncation; not OK (fall out)
                    }
                } else if (toType instanceof SignedIntegerType) {
                    // OK in general but needs to be zero-extended first
                    return bitCast(super.extend(from, ((SignedIntegerType) toType).asUnsigned()), toType);
                }
                // otherwise not OK (fall out)
            } else if (fromType instanceof FloatType) {
                if (toType instanceof FloatType) {
                    if (fromType.getMinBits() < toType.getMinBits()) {
                        // OK
                        return super.truncate(from, toType);
                    } else if (fromType.getMinBits() == toType.getMinBits()) {
                        // no actual extension needed
                        return from;
                    } else {
                        assert fromType.getMinBits() < toType.getMinBits();
                        // actually a truncation; not OK (fall out)
                    }
                }
            }
        }
        // report the error but produce the node anyway and continue
        ctxt.error(getLocation(), "Invalid extension of %s to %s", fromTypeRaw, toType);
        return super.extend(from, toType);
    }

    public Value bitCast(final Value from, final WordType toType) {
        ValueType fromTypeRaw = from.getType();
        if (fromTypeRaw.equals(toType)) {
            // no bitcast needed
            return from;
        }
        if (fromTypeRaw instanceof WordType) {
            WordType fromType = (WordType) fromTypeRaw;
            if (fromType.getMinBits() == toType.getMinBits()) {
                // OK
                return super.bitCast(from, toType);
            }
        }
        ctxt.error(getLocation(), "Invalid bitcast from %s to %s", fromTypeRaw, toType);
        return super.bitCast(from, toType);
    }

    public Value valueConvert(final Value from, final WordType toTypeRaw) {
        ValueType fromTypeRaw = from.getType();
        if (fromTypeRaw instanceof FloatType) {
            FloatType fromType = (FloatType) fromTypeRaw;
            if (toTypeRaw instanceof IntegerType) {
                IntegerType toType = (IntegerType) toTypeRaw;
                // insert a bounds check
                boolean signed = toType instanceof SignedIntegerType;
                // the highest allowed value (inclusive)
                double upper;
                // the lowest allowed value (inclusive)
                double lower;
                // subtracting 1.0 should be safe here since the ulp for these values should always be smaller than 1.0...
                if (signed) {
                    upper = Math.scalb(1.0, toType.getMinBits() - 1) - 1.0;
                    lower = -Math.scalb(1.0, toType.getMinBits() - 1);
                } else {
                    upper = Math.scalb(1.0, toType.getMinBits()) - 1.0;
                    lower = 0;
                }
                LiteralFactory lf = ctxt.getLiteralFactory();
                FloatLiteral upperLit, lowerLit;
                if (fromType.getMinBits() == 32) {
                    upperLit = lf.literalOf((float) upper);
                    lowerLit = lf.literalOf((float) lower);
                } else {
                    upperLit = lf.literalOf(upper);
                    lowerLit = lf.literalOf(lower);
                }

                // create the necessary Java bounds check

                final BlockLabel overMax = new BlockLabel();
                final BlockLabel notOverMax = new BlockLabel();
                final BlockLabel underMin = new BlockLabel();
                final BlockLabel resume = new BlockLabel();
                if_(cmpGt(from, upperLit), overMax, notOverMax);
                begin(overMax);
                goto_(resume);
                begin(notOverMax);
                if_(cmpLt(from, lowerLit), underMin, resume);
                begin(underMin);
                goto_(resume);
                begin(resume);
                PhiValue result = phi(toType, resume);
                result.setValueForBlock(ctxt, getCurrentElement(), overMax, upperLit);
                result.setValueForBlock(ctxt, getCurrentElement(), underMin, lowerLit);
                result.setValueForBlock(ctxt, getCurrentElement(), notOverMax, super.valueConvert(from, toType));
                return result;
            }
        } else if (fromTypeRaw instanceof IntegerType) {
            if (toTypeRaw instanceof FloatType) {
                // no bounds check needed in this case
                return super.valueConvert(from, toTypeRaw);
            }
        }
        ctxt.error(getLocation(), "Invalid conversion from %s to %s", fromTypeRaw, toTypeRaw);
        return super.valueConvert(from, toTypeRaw);
    }
}
