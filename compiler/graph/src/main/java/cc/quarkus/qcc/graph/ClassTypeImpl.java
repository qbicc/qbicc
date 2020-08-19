package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class ClassTypeImpl extends AbstractClassTypeImpl implements ClassType {
    private final VerifiedTypeDefinition definition;
    private final ClassType superClass;
    private final InterfaceType[] interfaces;

    ClassTypeImpl(final VerifiedTypeDefinition definition, final ClassType superClass, final InterfaceType[] interfaces) {
        this.definition = definition;
        this.superClass = superClass;
        this.interfaces = interfaces;
    }

    public String getClassName() {
        return definition.getInternalName();
    }

    public ClassType getSuperClass() {
        return superClass;
    }

    public int getInterfaceCount() {
        return interfaces.length;
    }

    public VerifiedTypeDefinition getDefinition() {
        return definition;
    }

    public InterfaceType getInterface(final int index) throws IndexOutOfBoundsException {
        return interfaces[index];
    }

    public boolean isSuperTypeOf(final ClassType other) {
        Assert.checkNotNullParam("other", other);

        if (superClass == null) {
            // all objects are assignable to JLO
            return true;
        }

        ClassType superClass = other.getSuperClass();

        return other == this || superClass != null && isAssignableFrom(superClass);
    }

    public int getParameterCount() {
        return 0;
    }

    public String getParameterName(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public Constraint getParameterConstraint(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
    }

    public String getLabelForGraph() {
        return "class[" + getClassName() + "]";
    }

    @Override
    public String toString() {
        return "ClassTypeImpl{" +
                "className='" + getClassName() + '\'' +
                '}';
    }
}
