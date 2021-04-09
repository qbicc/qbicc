package org.qbicc.type.generic;

import static org.qbicc.type.generic.Signature.*;

import java.nio.ByteBuffer;

/**
 * The explicit "any" type argument, encoded as {@code *}.
 */
public final class AnyTypeArgument extends TypeArgument {
    public static final AnyTypeArgument INSTANCE = new AnyTypeArgument();

    private AnyTypeArgument() {
        super(AnyTypeArgument.class.hashCode());
    }

    public boolean equals(final TypeArgument other) {
        return other instanceof AnyTypeArgument;
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append('*');
    }

    static TypeArgument parse(final ByteBuffer buf) {
        expect(buf, '*');
        return INSTANCE;
    }
}
