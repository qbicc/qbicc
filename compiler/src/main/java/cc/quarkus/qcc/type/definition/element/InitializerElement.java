package cc.quarkus.qcc.type.definition.element;

import java.util.List;

import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class InitializerElement extends BasicElement implements ExecutableElement {
    final MethodHandle methodBody;
    final DefinedTypeDefinition enclosingType;

    InitializerElement(Builder builder) {
        super(builder);
        this.methodBody = builder.methodBody;
        this.enclosingType = Assert.checkNotNullParam("builder.enclosingType", builder.enclosingType);
    }

    public boolean hasMethodBody() {
        return methodBody != null;
    }

    public MethodHandle getMethodBody() {
        return methodBody;
    }

    public FunctionType getType(final ClassContext classContext, final List<ParameterizedSignature> signatureContext) {
        TypeSystem ts = classContext.getTypeSystem();
        return ts.getFunctionType(ts.getVoidType());
    }

    public MethodDescriptor getDescriptor() {
        return MethodDescriptor.VOID_METHOD_DESCRIPTOR;
    }

    public MethodSignature getSignature() {
        return MethodSignature.VOID_METHOD_SIGNATURE;
    }

    public DefinedTypeDefinition getEnclosingType() {
        return enclosingType;
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends BasicElement.Builder implements ExecutableElement.Builder {
        MethodHandle methodBody;
        DefinedTypeDefinition enclosingType;

        Builder() {}

        public void setMethodBody(final MethodHandle methodHandle) {
            this.methodBody = methodHandle;
        }

        public void setEnclosingType(final DefinedTypeDefinition enclosingType) {
            this.enclosingType = Assert.checkNotNullParam("enclosingType", enclosingType);
        }

        public InitializerElement build() {
            return new InitializerElement(this);
        }
    }
}
