package cc.quarkus.qcc.type.definition.element;

/**
 *
 */
public abstract class BasicElement implements Element {
    private final String sourceFileName;
    private final int modifiers;
    private final int index;

    BasicElement() {
        sourceFileName = null;
        modifiers = 0;
        index = 0;
    }

    BasicElement(Builder builder) {
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

    public static abstract class Builder implements Element.Builder {
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

        public abstract BasicElement build();
    }
}
