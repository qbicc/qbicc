package cc.quarkus.qcc.type.definition.element;

import java.util.Objects;

import cc.quarkus.qcc.type.ValueType;
import io.smallrye.common.constraint.Assert;

final class ParameterElementImpl extends AbstractAnnotatedElement implements ParameterElement {
    private final String name;
    private final TypeResolver typeResolver;
    private final int typeResolverMethodArg;
    private final int typeResolverParamArg;
    private final int index;

    ParameterElementImpl(Builder builder) {
        super(builder);
        name = builder.name;
        typeResolver = Assert.checkNotNullParam("builder.typeResolver", builder.typeResolver);
        typeResolverMethodArg = builder.methodArg;
        typeResolverParamArg = builder.paramArg;
        index = builder.index;
    }

    public int getIndex() {
        return index;
    }

    public ValueType getType() {
        // this is expected to be efficient so we do not need to cache
        return typeResolver.resolveParameterType(typeResolverMethodArg, typeResolverParamArg);
    }

    public boolean hasClass2Type() {
        return typeResolver.hasClass2ParameterType(typeResolverMethodArg, typeResolverParamArg);
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

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    static final class Builder extends AbstractAnnotatedElement.Builder implements ParameterElement.Builder {
        String name;
        TypeResolver typeResolver;
        int index;
        int methodArg;
        int paramArg;

        public void setName(final String name) {
            this.name = name;
        }

        public void setResolver(final TypeResolver resolver, final int methodArg, final int paramArg) {
            this.typeResolver = Assert.checkNotNullParam("resolver", resolver);
            this.methodArg = methodArg;
            this.paramArg = paramArg;
        }

        public void setIndex(final int index) {
            this.index = index;
        }

        public ParameterElementImpl build() {
            return new ParameterElementImpl(this);
        }
    }
}
