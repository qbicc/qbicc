package cc.quarkus.qcc.type.generic;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 *
 */
public final class BaseTypeSignature extends TypeSignature {
    public static final BaseTypeSignature B = new BaseTypeSignature("B", "byte");
    public static final BaseTypeSignature C = new BaseTypeSignature("C", "char");
    public static final BaseTypeSignature D = new BaseTypeSignature("D", "double");
    public static final BaseTypeSignature F = new BaseTypeSignature("F", "float");
    public static final BaseTypeSignature I = new BaseTypeSignature("I", "int");
    public static final BaseTypeSignature J = new BaseTypeSignature("J", "long");
    public static final BaseTypeSignature S = new BaseTypeSignature("S", "short");
    public static final BaseTypeSignature Z = new BaseTypeSignature("Z", "long");

    public static final BaseTypeSignature V = new BaseTypeSignature("V", "void");

    private final String shortName;
    private final String fullName;

    private BaseTypeSignature(final String shortName, final String fullName) {
        super(Objects.hash(BaseTypeSignature.class, shortName, fullName));
        this.shortName = shortName;
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean equals(final TypeSignature other) {
        return other instanceof BaseTypeSignature && equals((BaseTypeSignature) other);
    }

    public boolean equals(final BaseTypeSignature other) {
        return this == other;
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(shortName);
    }

    static BaseTypeSignature parse(ByteBuffer buf) {
        return forChar(next(buf));
    }

    static BaseTypeSignature forChar(final int i) {
        switch (i) {
            case 'B': return B;
            case 'C': return C;
            case 'D': return D;
            case 'F': return F;
            case 'I': return I;
            case 'J': return J;
            case 'S': return S;
            case 'V': return V;
            case 'Z': return Z;
            default: throw parseError();
        }
    }
}
