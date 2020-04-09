package cc.quarkus.qcc.metaprogram.jvm.signature;

/**
 *
 */
final class ArrayTypeSignatureImpl implements ArrayTypeSignature {
    private final TypeSignature memberSignature;

    ArrayTypeSignatureImpl(final TypeSignature memberSignature) {
        this.memberSignature = memberSignature;
    }

    public TypeSignature getMemberSignature() {
        return memberSignature;
    }

    public StringBuilder toString(final StringBuilder b) {
        return memberSignature.toString(b.append("array of (")).append(')');
    }

    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
