package org.qbicc.object;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.MemberElement;

/**
 *
 */
public abstract class Declaration extends ProgramObject {
    protected final MemberElement originalElement;
    private final ProgramModule programModule;

    Declaration(final Declaration original) {
        super(original);
        this.originalElement = original.getOriginalElement();
        this.programModule = original.programModule;
    }

    Declaration(final SectionObject original) {
        super(original);
        this.originalElement = original.getOriginalElement();
        this.programModule = original.getProgramModule();
    }

    Declaration(final MemberElement originalElement, ProgramModule programModule, final String name, final ValueType valueType) {
        super(name, valueType);
        this.originalElement = originalElement;
        this.programModule = programModule;
    }

    public MemberElement getOriginalElement() {
        return originalElement;
    }

    @Override
    public ProgramModule getProgramModule() {
        return programModule;
    }
}
