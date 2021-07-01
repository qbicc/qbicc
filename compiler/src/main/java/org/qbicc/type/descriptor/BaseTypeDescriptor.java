package org.qbicc.type.descriptor;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 *
 */
public final class BaseTypeDescriptor extends TypeDescriptor {
    public static final BaseTypeDescriptor B = new BaseTypeDescriptor('B', "byte", false);
    public static final BaseTypeDescriptor C = new BaseTypeDescriptor('C', "char", false);
    public static final BaseTypeDescriptor D = new BaseTypeDescriptor('D', "double", true);
    public static final BaseTypeDescriptor F = new BaseTypeDescriptor('F', "float", false);
    public static final BaseTypeDescriptor I = new BaseTypeDescriptor('I', "int", false);
    public static final BaseTypeDescriptor J = new BaseTypeDescriptor('J', "long", true);
    public static final BaseTypeDescriptor S = new BaseTypeDescriptor('S', "short", false);
    public static final BaseTypeDescriptor Z = new BaseTypeDescriptor('Z', "boolean", false);

    public static final BaseTypeDescriptor V = new BaseTypeDescriptor('V', "void", false);

    private final char shortName;
    private final String fullName;
    private final boolean class2;

    private BaseTypeDescriptor(final char shortName, final String fullName, final boolean class2) {
        super(Objects.hash(BaseTypeDescriptor.class, Character.valueOf(shortName), fullName));
        this.shortName = shortName;
        this.fullName = fullName;
        this.class2 = class2;
    }

    public char getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isClass2() {
        return class2;
    }

    public boolean isVoid() { return shortName == 'V'; }

    public boolean equals(final TypeDescriptor other) {
        return other instanceof BaseTypeDescriptor && equals((BaseTypeDescriptor) other);
    }

    public boolean equals(final BaseTypeDescriptor other) {
        return super.equals(other) && shortName == other.shortName && fullName.equals(other.fullName);
    }

    public StringBuilder toString(final StringBuilder target) {
        return target.append(shortName);
    }

    public static BaseTypeDescriptor parse(ByteBuffer buf) {
        return forChar((char) next(buf));
    }

    public static BaseTypeDescriptor forChar(final char c) {
        switch (c) {
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
