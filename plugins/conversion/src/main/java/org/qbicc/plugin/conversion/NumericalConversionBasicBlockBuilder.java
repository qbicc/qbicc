package org.qbicc.plugin.conversion;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.PhiValue;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.FloatLiteral;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.BooleanType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;

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
                    } else if (fromType.getMinBits() <= toType.getMinBits()) {
                        // no actual truncation needed
                        return from;
                    }
                } else if (toType instanceof UnsignedIntegerType) {
                    // OK in general but needs to be converted first
                    return truncate(bitCast(from, ((SignedIntegerType) fromType).asUnsigned()), toType);
                } else if (toType instanceof BooleanType) {
                    return super.truncate(from, toType);
                }
                // otherwise not OK (fall out)
            } else if (fromType instanceof UnsignedIntegerType) {
                if (toType instanceof UnsignedIntegerType) {
                    if (fromType.getMinBits() > toType.getMinBits()) {
                        // OK
                        return super.truncate(from, toType);
                    } else if (fromType.getMinBits() <= toType.getMinBits()) {
                        // no actual truncation needed
                        return from;
                    }
                } else if (toType instanceof SignedIntegerType) {
                    // OK in general but needs to be converted first
                    return truncate(bitCast(from, ((UnsignedIntegerType) fromType).asSigned()), toType);
                } else if (toType instanceof BooleanType) {
                    return super.truncate(from, toType);
                }
                // otherwise not OK (fall out)
            } else if (fromType instanceof FloatType) {
                if (toType instanceof FloatType) {
                    if (fromType.getMinBits() > toType.getMinBits()) {
                        // OK
                        return super.truncate(from, toType);
                    } else if (fromType.getMinBits() <= toType.getMinBits()) {
                        // no actual truncation needed
                        return from;
                    }
                }
            } else if (fromType instanceof BooleanType) {
                if (toType instanceof BooleanType) {
                    // no actual truncation needed
                    return from;
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
                    } else if (fromType.getMinBits() >= toType.getMinBits()) {
                        // no actual extension needed
                        return from;
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
            } else if ((fromType instanceof UnsignedIntegerType) || (fromType instanceof BooleanType)) {
                if (toType instanceof UnsignedIntegerType) {
                    if (fromType.getMinBits() < toType.getMinBits()) {
                        // OK
                        return super.extend(from, toType);
                    } else if (fromType.getMinBits() >= toType.getMinBits()) {
                        // no actual extension needed
                        return from;
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
                        return super.extend(from, toType);
                    } else if (fromType.getMinBits() >= toType.getMinBits()) {
                        // no actual extension needed
                        return from;
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
        if (from instanceof IntegerLiteral && ((IntegerLiteral) from).isZero()) {
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(toType);
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
                LiteralFactory lf = ctxt.getLiteralFactory();
                IntegerType toType = (IntegerType) toTypeRaw;
                // the lowest allowed value (inclusive)
                double lower = toType.getLowerInclusiveBound();
                // the highest allowed value (inclusive)
                double upper = toType.getUpperInclusiveBound();
                // the highest allowed value literal
                // it cannot be the same as the literal used in cmp cos they're different types
                Literal upperLit = lf.literalOf(toType, toType.getMaxValue());

                FloatLiteral upperLitCmp, lowerLit;
                if (fromType.getMinBits() == 32) {
                    upperLitCmp = lf.literalOf((float) upper);
                    lowerLit = lf.literalOf((float) lower);
                } else {
                    upperLitCmp = lf.literalOf(upper);
                    lowerLit = lf.literalOf(lower);
                }

                // create the necessary Java bounds check

                final BlockLabel overMax = new BlockLabel();
                final BlockLabel notOverMax = new BlockLabel();
                final BlockLabel underMin = new BlockLabel();
                final BlockLabel resume = new BlockLabel();
                if_(isGe(from, upperLitCmp), overMax, notOverMax);
                begin(overMax);
                goto_(resume);
                begin(notOverMax);
                if_(isLt(from, lowerLit), underMin, resume);
                begin(underMin);
                goto_(resume);
                begin(resume);
                PhiValue result = phi(toType, resume);

                result.setValueForBlock(ctxt, getCurrentElement(), overMax, super.valueConvert(upperLit, toType));
                result.setValueForBlock(ctxt, getCurrentElement(), underMin, super.valueConvert(lowerLit, toType));
                result.setValueForBlock(ctxt, getCurrentElement(), notOverMax, super.valueConvert(from, toType));
                return result;
            }
        } else if (fromTypeRaw instanceof IntegerType) {
            if (toTypeRaw instanceof FloatType) {
                // no bounds check needed in this case
                return super.valueConvert(from, toTypeRaw);
            } else if (toTypeRaw instanceof PointerType) {
                // pointer conversions are allowed
                if (fromTypeRaw.getSize() < toTypeRaw.getSize()) {
                    ctxt.error(getLocation(), "Invalid pointer conversion from narrower type %s", fromTypeRaw);
                }
                return super.valueConvert(from, toTypeRaw);
            }
        } else if (fromTypeRaw instanceof PointerType) {
            if (toTypeRaw instanceof IntegerType) {
                if (fromTypeRaw.getSize() > toTypeRaw.getSize()) {
                    ctxt.error(getLocation(), "Invalid pointer conversion to narrower type %s", fromTypeRaw);
                }
                return super.valueConvert(from, toTypeRaw);
            } else if (toTypeRaw instanceof ReferenceType) {
                return super.valueConvert(from, toTypeRaw);
            }
        } else if (fromTypeRaw instanceof ReferenceType) {
            if (toTypeRaw instanceof PointerType) {
                return super.valueConvert(from, toTypeRaw);
            }
        }
        ctxt.error(getLocation(), "Invalid conversion from %s to %s", fromTypeRaw, toTypeRaw);
        return super.valueConvert(from, toTypeRaw);
    }
}
