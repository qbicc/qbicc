package org.qbicc.object;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.MemberElement;

/**
 * An object which is part of a section.
 */
public abstract class SectionObject extends ProgramObject {
    final MemberElement originalElement;
    final ModuleSection moduleSection;

    SectionObject(final MemberElement originalElement, final String name, final ValueType valueType, ModuleSection moduleSection) {
        super(name, valueType);
        this.originalElement = originalElement;
        this.moduleSection = moduleSection;
    }

    /**
     * The program-level element that caused this SectionObject to be generated.
     * If the SectionObject is synthetic (injected by/for the qbicc runtime) originalElement will be null
     */
    public MemberElement getOriginalElement() {
        return originalElement;
    }

    @Override
    public ProgramModule getProgramModule() {
        return moduleSection.getProgramModule();
    }

    public abstract Declaration getDeclaration();

    public String toString() {
        return getName();
    }

    /**
     * Get the module section that this object belongs to.
     *
     * @return the module section (not {@code null})
     */
    public ModuleSection getModuleSection() {
        return moduleSection;
    }
}
