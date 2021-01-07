package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public abstract class BasicElement implements Element {
    private final DefinedTypeDefinition enclosingType;
    private final String sourceFileName;
    private final int modifiers;
    private final int index;

    BasicElement() {
        enclosingType = null;
        sourceFileName = null;
        modifiers = 0;
        index = 0;
    }

    BasicElement(Builder builder) {
        enclosingType = Assert.checkNotNullParam("builder.enclosingType", builder.enclosingType);
        sourceFileName = builder.sourceFileName;
        modifiers = builder.modifiers;
        index = builder.index;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public int getModifiers() {
        return modifiers;
    }

    public int getIndex() {
        return index;
    }

    public boolean hasAllModifiersOf(int mask) {
        return (getModifiers() & mask) == mask;
    }

    public boolean hasNoModifiersOf(int mask) {
        return (getModifiers() & mask) == mask;
    }

    public DefinedTypeDefinition getEnclosingType() {
        return enclosingType;
    }

    public static abstract class Builder implements Element.Builder {
        DefinedTypeDefinition enclosingType;
        String sourceFileName;
        int modifiers;
        int index;

        Builder() {}

        public void setSourceFileName(final String sourceFileName) {
            this.sourceFileName = sourceFileName;
        }

        public void setModifiers(final int modifiers) {
            this.modifiers = modifiers;
        }

        public void setIndex(final int index) {
            this.index = index;
        }

        public void setEnclosingType(final DefinedTypeDefinition enclosingType) {
            this.enclosingType = Assert.checkNotNullParam("enclosingType", enclosingType);
        }

        public abstract BasicElement build();
    }
}
