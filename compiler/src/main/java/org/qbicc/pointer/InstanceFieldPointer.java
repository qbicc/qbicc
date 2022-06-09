package org.qbicc.pointer;

import org.qbicc.interpreter.Memory;
import org.qbicc.type.definition.element.InstanceFieldElement;

/**
 *
 */
public final class InstanceFieldPointer extends Pointer {
    private final Pointer objectPointer;
    private final InstanceFieldElement fieldElement;

    public InstanceFieldPointer(Pointer objectPointer, InstanceFieldElement fieldElement) {
        super(fieldElement.getType().getPointer());
        this.objectPointer = objectPointer;
        this.fieldElement = fieldElement;
    }

    public Pointer getObjectPointer() {
        return objectPointer;
    }

    public InstanceFieldElement getInstanceField() {
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

    @Override
    public StringBuilder toString(StringBuilder b) {
        return objectPointer.toString(b).append('.').append(fieldElement.getName());
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
