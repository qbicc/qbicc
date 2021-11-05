package org.qbicc.object;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.MemberElement;

/**
 * A declaration of some global data item.
 */
public class DataDeclaration extends SectionObject {
    DataDeclaration(final MemberElement originalElement, final String name, final ValueType valueType) {
        super(originalElement, name, valueType);
    }

    DataDeclaration(final Data original) {
        super(original);
    }

    public MemberElement getOriginalElement() {
        return (MemberElement) super.getOriginalElement();
    }

    @Override
    public DataDeclaration getDeclaration() {
        return this;
    }
}
