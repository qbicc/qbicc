package org.qbicc.type.definition.element;

import org.qbicc.type.definition.classfile.ClassFile;

/**
 * An instance field element.
 */
public final class InstanceFieldElement extends FieldElement {
    InstanceFieldElement(BuilderImpl builder) {
        super(builder);
    }

    @Override
    public void setModifierFlags(int flags) {
        if ((flags & ClassFile.ACC_STATIC) != 0) {
            throw new IllegalArgumentException("Cannot make an instance element into a static element");
        }
        super.setModifierFlags(flags);
    }
}
