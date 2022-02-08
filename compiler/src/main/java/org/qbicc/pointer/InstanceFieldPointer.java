package org.qbicc.pointer;

import org.qbicc.interpreter.Memory;
import org.qbicc.type.definition.element.FieldElement;

/**
 *
 */
public final class InstanceFieldPointer extends Pointer {
    private final Pointer objectPointer;
    private final FieldElement fieldElement;

    public InstanceFieldPointer(Pointer objectPointer, FieldElement fieldElement) {
        super(fieldElement.getType().getPointer());
        this.objectPointer = objectPointer;
        this.fieldElement = fieldElement;
    }

    public Pointer getObjectPointer() {
        return objectPointer;
    }

    public FieldElement getFieldElement() {
        return fieldElement;
    }

    @Override
    public RootPointer getRootPointer() {
        return objectPointer.getRootPointer();
    }

    @Override
    public long getRootByteOffset() {
        return objectPointer.getRootByteOffset() + fieldElement.getOffset();
    }

    @Override
    public Memory getRootMemoryIfExists() {
        return objectPointer.getRootMemoryIfExists();
    }

    @Override
    public String getRootSymbolIfExists() {
        return objectPointer.getRootSymbolIfExists();
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
