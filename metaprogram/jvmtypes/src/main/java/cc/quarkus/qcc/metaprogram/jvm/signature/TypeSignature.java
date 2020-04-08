package cc.quarkus.qcc.metaprogram.jvm.signature;

import cc.quarkus.qcc.context.Context;

/**
 * The base type for all JVM-defined generic signature types.
 */
public interface TypeSignature {
    /**
     * Parse a type signature.  Requires an active {@link Context}.
     *
     * @param signature the signature string
     * @return the signature object
     * @throws IllegalArgumentException if the string is not valid
     */
    static TypeSignature parseTypeSignature(String signature) {
        return Parsing.parseTypeSignature(signature);
    }
}
