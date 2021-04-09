package org.qbicc.type.annotation;

import io.smallrye.common.constraint.Assert;

/**
 * A {@link String} annotation value.
 */
public final class StringAnnotationValue extends AnnotationValue {
    private final String string;

    StringAnnotationValue(final String string) {
        this.string = string;
    }

    public static StringAnnotationValue of(final String value) {
        return new StringAnnotationValue(Assert.checkNotNullParam("value", value));
    }

    public String getString() {
        return string;
    }

    public Kind getKind() {
        return Kind.STRING;
    }
}
