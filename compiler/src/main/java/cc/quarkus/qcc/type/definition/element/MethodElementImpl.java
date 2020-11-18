package cc.quarkus.qcc.type.definition.element;

import java.util.Objects;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import io.smallrye.common.constraint.Assert;

final class MethodElementImpl extends AbstractParameterizedExecutableElement implements MethodElement {
    private final String name;
    private final TypeResolver typeResolver;
    private final int typeResolverArg;
    private final MethodHandle virtualMethodBody;
    private volatile MethodDescriptor descriptor;

    MethodElementImpl(final Builder builder) {
        super(builder);
        name = builder.name;
        typeResolver = Assert.checkNotNullParam("builder.typeResolver", builder.typeResolver);
        typeResolverArg = builder.typeResolverArg;
        virtualMethodBody = builder.virtualMethodBody;
    }

    public ValueType getReturnType() {
        return getDescriptor().getReturnType();
    }

    public boolean hasClass2ReturnType() {
        return typeResolver.hasClass2ReturnType(typeResolverArg);
    }

    public MethodDescriptor getDescriptor() {
        MethodDescriptor descriptor = this.descriptor;
        if (descriptor == null) {
            descriptor = this.descriptor = typeResolver.resolveMethodDescriptor(typeResolverArg);
        }
        return descriptor;
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

    public String toString() {
        return getName() + ":" + getEnclosingType().getInternalName();
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    static final class Builder extends AbstractParameterizedExecutableElement.Builder implements MethodElement.Builder {
        String name;
        TypeResolver typeResolver;
        int typeResolverArg;
        MethodHandle virtualMethodBody;

        public void setName(final String name) {
            this.name = name;
        }

        public void setMethodTypeResolver(final TypeResolver resolver, final int argument) {
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
