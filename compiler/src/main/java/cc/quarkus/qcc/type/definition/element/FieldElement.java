package cc.quarkus.qcc.type.definition.element;

import java.util.List;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public final class FieldElement extends VariableElement implements MemberElement {
    public static final FieldElement[] NO_FIELDS = new FieldElement[0];

    private final DefinedTypeDefinition enclosingType;

    FieldElement(Builder builder) {
        super(builder);
        this.enclosingType = Assert.checkNotNullParam("builder.enclosingType", builder.enclosingType);
    }

    public boolean isVolatile() {
        return hasAllModifiersOf(ClassFile.ACC_VOLATILE);
    }

    public static Builder builder() {
        return new Builder();
    }

    public DefinedTypeDefinition getEnclosingType() {
        return enclosingType;
    }

    public ValueType getType(final ClassContext classContext, final List<ParameterizedSignature> signatureContext) {
        return null;
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static final class Builder extends VariableElement.Builder implements MemberElement.Builder {
        DefinedTypeDefinition enclosingType;

        Builder() {}

        public void setEnclosingType(final DefinedTypeDefinition enclosingType) {
            this.enclosingType = Assert.checkNotNullParam("enclosingType", enclosingType);
        }

        public FieldElement build() {
            return new FieldElement(this);
        }
    }
}
