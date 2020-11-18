package cc.quarkus.qcc.type.generic;

/**
 *
 */
final class TypeVariableSignatureImpl implements TypeVariableSignature {
    private final String simpleName;

    TypeVariableSignatureImpl(final String simpleName) {
        this.simpleName = simpleName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public StringBuilder toString(final StringBuilder b) {
        return b.append("type variable ").append(simpleName);
    }

    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
