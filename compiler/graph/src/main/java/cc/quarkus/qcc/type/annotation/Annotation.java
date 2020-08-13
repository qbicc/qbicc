package cc.quarkus.qcc.type.annotation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.constraint.Assert;

/**
 * An annotation.
 */
public final class Annotation extends AnnotationValue {
    private final String className;
    private final Map<String, AnnotationValue> values;

    Annotation(Builder builder) {
        className = Assert.checkNotNullParam("builder.className", builder.className);
        values = Collections.unmodifiableMap(new LinkedHashMap<>(builder.values));
    }

    public String getClassName() {
        return className;
    }

    public AnnotationValue getValue(String name) {
        return values.get(name);
    }

    public AnnotationValue getValue(String name, AnnotationValue defaultVal) {
        return values.getOrDefault(name, defaultVal);
    }

    public Set<String> getNames() {
        return values.keySet();
    }

    public Kind getKind() {
        return Kind.ANNOTATION;
    }

    public static final class Builder {
        String className;
        HashMap<String, AnnotationValue> values = new LinkedHashMap<>();

        Builder() {}

        public String getClassName() {
            return className;
        }

        public Builder setClassName(final String className) {
            this.className = Assert.checkNotNullParam("className", className);
            return this;
        }

        public Builder addValue(String name, AnnotationValue value) {
            values.putIfAbsent(Assert.checkNotNullParam("name", name), Assert.checkNotNullParam("value", value));
            return this;
        }

        public Annotation build() {
            return new Annotation(this);
        }
    }
}
