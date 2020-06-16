package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.constraint.Constraint;

final class ArrayClassTypeImpl extends AbstractClassTypeImpl implements ArrayClassType {
    private final Type elementType;

    ArrayClassTypeImpl(final Type elementType) {
        this.elementType = elementType;
    }

    public Type getElementType() {
        return elementType;
    }

    public String getClassName() {
        // todo
        return "array of something";
    }

    public ClassType getSuperClass() {
        return Type.JAVA_LANG_OBJECT;
    }

    public int getInterfaceCount() {
        return 0;
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
