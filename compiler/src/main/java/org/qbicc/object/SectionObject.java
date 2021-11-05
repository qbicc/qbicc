package org.qbicc.object;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.Element;

/**
 * An object which is part of a section.
 */
public abstract class SectionObject extends ProgramObject {
    final Element originalElement;

    SectionObject(final Element originalElement, final String name, final ValueType valueType) {
        super(name, valueType);
        this.originalElement = originalElement;
    }

    SectionObject(final SectionObject original) {
        super(original);
        this.originalElement = original.getOriginalElement();
    }

    /**
     * The program-level element that caused this SectionObject to be generated.
     * If the SectionObject is synthetic (injected by/for the qbicc runtime) originalElement will be null
     */
    public Element getOriginalElement() {
        return originalElement;
    }

    public abstract SectionObject getDeclaration();

    public String toString() {
        return getName();
    }
}
