package org.qbicc.pointer;

import org.qbicc.type.definition.element.StaticFieldElement;

/**
 * A pointer to a static field.
 */
public final class StaticFieldPointer extends RootPointer {
    private final StaticFieldElement staticField;

    StaticFieldPointer(StaticFieldElement staticField) {
        super(staticField.getType().getPointer());
        this.staticField = staticField;
    }

    public static StaticFieldPointer of(final StaticFieldElement fieldElement) {
        return fieldElement.getOrCreatePointer(StaticFieldPointer::new);
    }

    public StaticFieldElement getStaticField() {
        return staticField;
    }

    @Override
    public long getRootByteOffset() {
        return staticField.getOffset();
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + staticField.hashCode();
    }

    @Override
    public boolean equals(final RootPointer other) {
        return other instanceof StaticFieldPointer sfp && equals(sfp);
    }

    public boolean equals(final StaticFieldPointer other) {
        return super.equals(other) && staticField == other.staticField;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('&').append(staticField.getEnclosingType().getInternalName()).append('#').append(staticField.getName());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T t) {
        return visitor.visit(t, this);
    }
}
