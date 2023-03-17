package org.qbicc.object;

import org.qbicc.graph.Value;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.MemberElement;

/**
 * A data object definition.
 */
public final class Data extends SectionObject {
    private final Value value;
    private volatile DataDeclaration declaration;
    private volatile boolean dsoLocal;
    private volatile boolean constant;
    private volatile long offset;

    Data(final MemberElement originalElement, ModuleSection moduleSection, final String name, final ValueType valueType, final Value value) {
        super(originalElement, name, valueType, moduleSection);
        this.value = value;
        offset = -1;
    }

    @Override
    public MemberElement getOriginalElement() {
        return super.getOriginalElement();
    }

    public String getName() {
        return name;
    }

    public Value getValue() {
        return value;
    }

    /**
     * Get this object's offset within its enclosing module section.
     *
     * @return the relative offset
     */
    public long getOffset() {
        long offset = this.offset;
        if (offset == -1) {
            throw new IllegalArgumentException("Offset unknown");
        }
        return offset;
    }

    void initOffset(long offset) {
        this.offset = offset;
    }

    /**
     * Get this object's actual size.
     *
     * @return the object's actual size
     */
    public long getSize() {
        return value.getType().getSize();
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

    public boolean isConstant() {
        return constant;
    }

    public void setConstant(boolean constant) {
        this.constant = constant;
    }
}
