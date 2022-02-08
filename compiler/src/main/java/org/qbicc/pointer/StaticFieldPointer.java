package org.qbicc.pointer;

import org.qbicc.type.definition.element.FieldElement;

/**
 * A pointer to a static field.
 */
public final class StaticFieldPointer extends RootPointer {
    private final FieldElement staticField;

    StaticFieldPointer(FieldElement staticField) {
        super(staticField.getType().getPointer());
        if (! staticField.isStatic()) {
            throw new IllegalArgumentException("Pointer to non-static field");
        }
        this.staticField = staticField;
    }

    public static StaticFieldPointer of(final FieldElement fieldElement) {
        return fieldElement.getOrCreatePointer(StaticFieldPointer::new);
    }

    public FieldElement getStaticField() {
        return staticField;
    }

    @Override
    public long getRootByteOffset() {
        return staticField.getOffset();
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T t) {
        return visitor.visit(t, this);
    }
}
