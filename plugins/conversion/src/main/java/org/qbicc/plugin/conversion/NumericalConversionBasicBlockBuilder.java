package org.qbicc.plugin.conversion;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.type.BooleanType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.TypeIdType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;

/**
 * This builder fixes up mismatched numerical conversions in order to avoid duplicating this kind of logic in other
 * builders.
 */
public class NumericalConversionBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NumericalConversionBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
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
                } else if (toType instanceof TypeIdType) {
                    if (fromType.getMinBits() > toType.getMinBits()) {
                        return super.truncate(from, toType);
                    } else {
                        return from;
                    }
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
                    if (fromType.getMinBits() < toType.getMinBits() || fromType instanceof BooleanType) {
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
            } else if (fromType instanceof TypeIdType) {
                if (toType instanceof IntegerType) {
                    if (fromType.getMinBits() < toType.getMinBits()) {
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
}
