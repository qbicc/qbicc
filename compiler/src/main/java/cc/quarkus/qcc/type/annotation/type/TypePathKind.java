package cc.quarkus.qcc.type.annotation.type;

/**
 *
 */
public enum TypePathKind {
    ARRAY,
    NESTED,
    WILDCARD_BOUND,
    TYPE_ARGUMENT,
    ;

    private static final TypePathKind[] VALUES = values();

    public static TypePathKind of(final int idx) {
        return VALUES[idx];
    }
}
