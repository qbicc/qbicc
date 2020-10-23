package cc.quarkus.qcc.type.definition.element;

import java.util.Objects;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import io.smallrye.common.constraint.Assert;

final class FieldElementImpl extends AbstractAnnotatedElement implements FieldElement {
    private final String name;
    private final TypeResolver typeResolver;
    private final long typeResolverArg;
    private volatile ValueType type;

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

    public ValueType getType() throws ResolutionFailedException {
        ValueType type = this.type;
        if (type == null) {
            this.type = type = typeResolver.resolveFieldType(typeResolverArg);
        }
        return type;
    }

    public boolean hasClass2Type() {
        return typeResolver.hasClass2FieldType(typeResolverArg);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
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
