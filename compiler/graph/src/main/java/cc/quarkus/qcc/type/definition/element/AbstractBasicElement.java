package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
abstract class AbstractBasicElement implements BasicElement {
    private final DefinedTypeDefinition enclosingType;
    private final int access;

    AbstractBasicElement(Builder builder) {
        this.access = builder.access;
        this.enclosingType = Assert.checkNotNullParam("builder.enclosingType", builder.enclosingType);
    }

    public DefinedTypeDefinition getEnclosingType() {
        return enclosingType;
    }

    public int getModifiers() {
        return access;
    }

    static abstract class Builder implements BasicElement.Builder {
        int access;
        DefinedTypeDefinition enclosingType;

        public void setModifiers(final int access) {
            this.access = access;
        }

        public void setEnclosingType(final DefinedTypeDefinition enclosingType) {
            this.enclosingType = Assert.checkNotNullParam("enclosingType", enclosingType);
        }

        public abstract AbstractBasicElement build();
    }
}
