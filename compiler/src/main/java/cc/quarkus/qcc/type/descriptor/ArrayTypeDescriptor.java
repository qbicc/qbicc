package cc.quarkus.qcc.type.descriptor;

import cc.quarkus.qcc.type.ObjectReference;

public class ArrayTypeDescriptor implements TypeDescriptor<ObjectReference> {
    ArrayTypeDescriptor(TypeDescriptor<?> elementType) {
        this.elementType = elementType;
    }

    @Override
    public TypeDescriptor<?> baseType() {
        return TypeDescriptor.OBJECT;
    }

    @Override
    public Class<ObjectReference> type() {
        return ObjectReference.class;
    }

    @Override
    public String label() {
        return this.elementType.label() + "[]";
    }

    private final TypeDescriptor<?> elementType;
}
