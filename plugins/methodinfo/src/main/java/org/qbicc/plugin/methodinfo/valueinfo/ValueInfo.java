package org.qbicc.plugin.methodinfo.valueinfo;

/**
 * An object which specifies where a value resides at a given call site.
 */
public abstract class ValueInfo {
    private final int hashCode;

    ValueInfo(int hashCode) {
        this.hashCode = getClass().hashCode() * 19 + hashCode;
    }

    public final int hashCode() {
        return hashCode;
    }

    public final boolean equals(Object other) {
        return other instanceof ValueInfo vi && equals(vi);
    }

    public boolean equals(ValueInfo other) {
        return this == other || other != null && hashCode == other.hashCode;
    }
}
