package org.qbicc.object;

import org.qbicc.graph.Value;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.MemberElement;

/**
 * A data object definition.
 */
public final class Data extends SectionObject {
    private volatile Value value;
    private volatile DataDeclaration declaration;
    private volatile boolean dsoLocal;

    Data(final MemberElement originalElement, final String name, final ValueType valueType, final Value value) {
        super(originalElement, name, valueType);
        this.value = value;
    }

    @Override
    public MemberElement getOriginalElement() {
        return (MemberElement) super.getOriginalElement();
    }

    public String getName() {
        return name;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(final Value value) {
        this.value = value;
    }

    public DataDeclaration getDeclaration() {
        DataDeclaration declaration = this.declaration;
        if (declaration == null) {
            synchronized (this) {
                declaration = this.declaration;
                if (declaration == null) {
                    declaration = this.declaration = new DataDeclaration(this);
                }
            }
        }
        return declaration;
    }

    void initDeclaration(DataDeclaration decl) {
        declaration = decl;
    }

    public void setDsoLocal() {
        dsoLocal = true;
    }

    public boolean isDsoLocal() {
        return dsoLocal;
    }
}
