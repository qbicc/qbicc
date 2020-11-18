package cc.quarkus.qcc.type.generic;

/**
 * A package name.
 */
public interface PackageName {
    String getSimpleName();

    PackageName getEnclosing();

    boolean hasEnclosing();

    default StringBuilder appendQualifiedName(StringBuilder b) {
        if (hasEnclosing()) {
            getEnclosing().appendQualifiedName(b).append('.');
        }
        b.append(getSimpleName());
        return b;
    }

    default boolean qualifiedNameEquals(String name) {
        return qualifiedNameEquals(name, 0, name.length());
    }

    default boolean qualifiedNameEquals(String name, int start, int len) {
        int idx = name.lastIndexOf('.', start + len);
        if (idx == -1) {
            return ! hasEnclosing() && getSimpleName().regionMatches(0, name, start, len);
        } else {
            return hasEnclosing() && getEnclosing().qualifiedNameEquals(name, start, idx - start) && getSimpleName().regionMatches(0, name, idx + 1, idx - start);
        }
    }
}
