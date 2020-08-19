package cc.quarkus.qcc.type.definition.element;

import java.util.Objects;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import io.smallrye.common.constraint.Assert;

final class FieldElementImpl extends AbstractAnnotatedElement implements FieldElement {
    private final String name;
    private final TypeResolver typeResolver;
    private final long typeResolverArg;
    private volatile Type type;

    FieldElementImpl(final Builder builder) {
        super(builder);
        name = builder.name;
        typeResolver = Assert.checkNotNullParam("builder.typeResolver", builder.typeResolver);
        typeResolverArg = builder.typeResolverArg;
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

    public Type getType() throws ResolutionFailedException {
        Type type = this.type;
        if (type == null) {
            this.type = type = typeResolver.resolveFieldType(typeResolverArg);
        }
        return type;
    }

    static final class Builder extends AbstractAnnotatedElement.Builder implements FieldElement.Builder {
        String name;
        TypeResolver typeResolver;
        long typeResolverArg;

        public void setName(final String name) {
            this.name = name;
        }

        public void setTypeResolver(final TypeResolver resolver, final long argument) {
            this.typeResolver = Assert.checkNotNullParam("resolver", resolver);
            this.typeResolverArg = argument;
        }

        public FieldElementImpl build() {
            return new FieldElementImpl(this);
        }
    }
}
