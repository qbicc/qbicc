package cc.quarkus.qcc.type.definition.element;

import java.util.Objects;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.MethodHandle;
import io.smallrye.common.constraint.Assert;

final class MethodElementImpl extends AbstractParameterizedExecutableElement implements MethodElement {
    private final String name;
    private final TypeResolver typeResolver;
    private final long typeResolverArg;
    private final MethodHandle virtualMethodBody;
    private volatile Type returnType;

    MethodElementImpl(final Builder builder) {
        super(builder);
        name = builder.name;
        typeResolver = Assert.checkNotNullParam("builder.typeResolver", builder.typeResolver);
        typeResolverArg = builder.typeResolverArg;
        virtualMethodBody = builder.virtualMethodBody;
    }

    public Type getReturnType() {
        Type returnType = this.returnType;
        if (returnType == null) {
            returnType = this.returnType = typeResolver.resolveMethodReturnType(typeResolverArg);
        }
        return returnType;
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

    public boolean hasVirtualMethodBody() {
        return virtualMethodBody != null;
    }

    public MethodHandle getVirtualMethodBody() {
        return virtualMethodBody;
    }

    static final class Builder extends AbstractParameterizedExecutableElement.Builder implements MethodElement.Builder {
        String name;
        TypeResolver typeResolver;
        long typeResolverArg;
        MethodHandle virtualMethodBody;

        public void setName(final String name) {
            this.name = name;
        }

        public void setReturnTypeResolver(final TypeResolver resolver, final long argument) {
            typeResolver = Assert.checkNotNullParam("resolver", resolver);
            typeResolverArg = argument;
        }

        public void setVirtualMethodBody(final MethodHandle handle) {
            virtualMethodBody = handle;
        }

        public MethodElementImpl build() {
            return new MethodElementImpl(this);
        }
    }
}
