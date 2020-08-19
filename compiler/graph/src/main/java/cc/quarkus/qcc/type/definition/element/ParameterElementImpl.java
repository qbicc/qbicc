package cc.quarkus.qcc.type.definition.element;

import java.util.Objects;

import cc.quarkus.qcc.graph.Type;
import io.smallrye.common.constraint.Assert;

final class ParameterElementImpl extends AbstractAnnotatedElement implements ParameterElement {
    private final String name;
    private final TypeResolver typeResolver;
    private final long typeResolverArg;
    private volatile Type type;

    ParameterElementImpl(Builder builder) {
        super(builder);
        name = builder.name;
        typeResolver = Assert.checkNotNullParam("builder.typeResolver", builder.typeResolver);
        typeResolverArg = builder.typeResolverArg;
    }

    public Type getType() {
        Type type = this.type;
        if (type == null) {
            type = this.type = typeResolver.resolveParameterType(typeResolverArg);
        }
        return type;
    }

    public boolean hasName() {
        return name != null;
    }

    public String getName() {
        return name;
    }

    public boolean nameEquals(final String name) {
        return Objects.equals(this.name, name);
    }

    static final class Builder extends AbstractAnnotatedElement.Builder implements ParameterElement.Builder {
        String name;
        TypeResolver typeResolver;
        long typeResolverArg;

        public void setName(final String name) {
            this.name = name;
        }

        public void setResolver(final TypeResolver resolver, final long argument) {
            this.typeResolver = Assert.checkNotNullParam("resolver", resolver);
            this.typeResolverArg = argument;
        }

        public ParameterElementImpl build() {
            return new ParameterElementImpl(this);
        }
    }
}
