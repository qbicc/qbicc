package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.interpreter.JavaVM;
import cc.quarkus.qcc.type.definition.VerifiedTypeDefinition;

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
        return JavaVM.requireCurrent().getObjectTypeDefinition().verify().getClassType();
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
}
