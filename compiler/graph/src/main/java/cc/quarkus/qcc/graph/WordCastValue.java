package cc.quarkus.qcc.graph;

/**
 *
 */
public interface WordCastValue extends CastValue {
    Kind getKind();
    void setKind(Kind kind);

    enum Kind {
        /**
         * Truncate a value.  For floating point types, the value is cast appropriately.  The output type must be
         * the same kind as the input type, and must be narrower than the input type.
         */
        TRUNCATE,
        /**
         * Extend a value.  Signed type values are sign-extended; floating point type values are widened.  The output
         * type must be the same kind as the input type, and must be wider than the input type.
         */
        EXTEND,
        /**
         * Bit-for-bit cast.  The output type must be the exact same width as the input type.
         */
        BIT_CAST,
        /**
         * Convert a floating point input to a signed or unsigned integer output, or vice-versa.  The value must be
         * in range for the target type, otherwise undefined behavior will result; thus, conversions should be preceded
         * by an appropriate range check if necessary.
         */
        VALUE_CONVERT,
    }
}
