package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.definition.classfile.ClassFile;

/**
 *
 */
public final class MethodElement extends InvokableElement implements NamedElement {
    public static final MethodElement[] NO_METHODS = new MethodElement[0];

    /**
     * Special marker method used in method searches.
     */
    public static final MethodElement NOT_FOUND = new MethodElement();
    /**
     * Special marker method used in method searches.
     */
    public static final MethodElement END_OF_SEARCH = new MethodElement();

    private final String name;

    MethodElement() {
        super();
        this.name = null;
    }

    MethodElement(Builder builder) {
        super(builder);
        this.name = builder.name;
    }

    public String toString() {
        return getEnclosingType().getDescriptor().getPackageName()+"."+getEnclosingType().getDescriptor().getClassName()+"."+getName()+getDescriptor();
    }

    public String getName() {
        return name;
    }

    public boolean isAbstract() {
        return hasAllModifiersOf(ClassFile.ACC_ABSTRACT);
    }

    public boolean isFinal() {
        return hasAllModifiersOf(ClassFile.ACC_FINAL);
    }

    public boolean isVirtual() {
        return hasNoModifiersOf(ClassFile.ACC_FINAL | ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends InvokableElement.Builder implements NamedElement.Builder {
        String name;

        Builder() {}

        public void setName(final String name) {
            this.name = name;
        }

        public MethodElement build() {
            return new MethodElement(this);
        }
    }
}
