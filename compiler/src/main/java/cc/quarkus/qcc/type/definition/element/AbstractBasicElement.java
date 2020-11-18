package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
abstract class AbstractBasicElement implements BasicElement {
    private final DefinedTypeDefinition enclosingType;
    private final String sourceFile;
    private final int access;

    AbstractBasicElement(Builder builder) {
        this.enclosingType = Assert.checkNotNullParam("builder.enclosingType", builder.enclosingType);
        this.sourceFile = builder.sourceFile;
        this.access = builder.access;
    }

    public String getSourceFileName() {
        return sourceFile;
    }

    public DefinedTypeDefinition getEnclosingType() {
        return enclosingType;
    }

    public int getModifiers() {
        return access;
    }

    static abstract class Builder implements BasicElement.Builder {
        String sourceFile;
        int access;
        DefinedTypeDefinition enclosingType;

        public void setSourceFile(final String sourceFile) {
            this.sourceFile = sourceFile;
        }

        public void setModifiers(final int access) {
            this.access = access;
        }

        public void setEnclosingType(final DefinedTypeDefinition enclosingType) {
            this.enclosingType = Assert.checkNotNullParam("enclosingType", enclosingType);
        }

        public abstract AbstractBasicElement build();
    }
}
