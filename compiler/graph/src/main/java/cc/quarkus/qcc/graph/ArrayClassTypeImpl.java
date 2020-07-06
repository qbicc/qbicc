package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;
import cc.quarkus.qcc.type.universe.Universe;

final class ArrayClassTypeImpl extends AbstractClassTypeImpl implements ArrayClassType {
    private final Type elementType;
    private final ClassType superClass;

    ArrayClassTypeImpl(final Type elementType) {
        this.elementType = elementType;
        superClass = Universe.rootUniverse().findClass("java/lang/Object").verify().getClassType();
    }

    public Type getElementType() {
        return elementType;
    }

    public String getClassName() {
        // todo
        return "array of something";
    }

    public ClassType getSuperClass() {
        return superClass;
    }

    public int getInterfaceCount() {
        return 0;
    }

    public VerifiedTypeDefinition getDefinition() {
        throw new UnsupportedOperationException("TODO: hard code array class impls");
    }

    public InterfaceType getInterface(final int index) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException(index);
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
        return "array[" + elementType.getLabelForGraph() + "]";
    }
}
