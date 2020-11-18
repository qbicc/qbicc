package cc.quarkus.qcc.type.annotation;

import java.util.Collection;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public abstract class AnnotationValue {
    private static final AnnotationValue[] NO_VALUES = new AnnotationValue[0];

    AnnotationValue() {}

    public static ArrayAnnotationValue array(AnnotationValue... values) {
        return new ArrayAnnotationValue(Assert.checkNotNullParam("values", values).clone());
    }

    public static ArrayAnnotationValue array(Collection<AnnotationValue> values) {
        return new ArrayAnnotationValue(Assert.checkNotNullParam("values", values).toArray(NO_VALUES));
    }

    public enum Kind {
        BYTE,
        CHAR,
        DOUBLE,
        FLOAT,
        INT,
        LONG,
        SHORT,
        BOOLEAN,
        STRING,
        ENUM,
        CLASS,
        ANNOTATION,
        ARRAY,
        ;
    }

    public abstract Kind getKind();
}
